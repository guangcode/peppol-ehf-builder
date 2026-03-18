# EHF Invoice Builder

A Java library for generating **EHF 3.0 / PEPPOL BIS Billing 3.0** compliant invoices, with full support for Norwegian tax rules and B2G (Business-to-Government) requirements.

> Built on top of [ph-ubl](https://github.com/phax/ph-ubl) — no XML template files, no string manipulation. Pure UBL 2.1 object model construction.

---

## Why This Library Exists

Generating a PEPPOL-compliant invoice is not just about producing valid XML. The real challenge is the **Norwegian localization layer**:

- Norwegian MVA-koder (e.g., `3`, `5`, `6`, `31`) must be translated to PEPPOL UNCL5305 tax category codes (`S`, `Z`, `E`, `AE`, etc.)
- The supplier's VAT ID must follow the exact format: `NO` + OrgNr + `MVA`
- `BuyerReference` is effectively mandatory for B2G (government) invoicing — missing it causes silent rejection by municipal ERP systems
- KID (Kundeidentifikasjonsnummer) for OCR-based bank reconciliation must be mapped to `PaymentID`, not `PaymentNote`
- Tax subtotals must be grouped strictly by category **and** rate (BR-CO-17) — merging two zero-rate categories will fail Schematron validation

This library encodes all of the above so you don't have to rediscover it.

---

## Requirements

| Dependency | Version |
|---|---|
| Java | 11+ |
| ph-ubl | 9.x |
| Lombok | 1.18+ |

```xml
<!-- Maven dependency for ph-ubl -->
<dependency>
    <groupId>com.helger</groupId>
    <artifactId>ph-ubl21</artifactId>
    <version>9.x.x</version>
</dependency>
```

---

## Quick Start

```java
// 1. Build the DTO
EhfInvoiceDTO dto = new EhfInvoiceDTO();
dto.setInvoiceNo("INV-2024-001");
dto.setInvoiceDate(LocalDate.of(2024, 5, 1));
dto.setInvoiceDueDate(LocalDate.of(2024, 5, 31));
dto.setCurrency("NOK");
dto.setBuyerReference("Morten Tveten");           // Critical for B2G
dto.setCompanyOrganizationNo("123456789");
dto.setCompanyName("Supplier AS");
dto.setCompanyLegalName("Supplier AS");
dto.setCompanyCountryCode("NO");
dto.setIsVatRegistered(true);                     // Triggers NO...MVA format
dto.setCustomerOrgCode("987654321");
dto.setCustomerName("Buyer Kommune");
dto.setCustomerCountryCode("NO");
// ... set amounts, line items, VAT details

// 2. Generate the UBL object
EhfInvoiceFactory factory = new EhfInvoiceFactory();
InvoiceType invoice = factory.createInvoice(dto, null);

// 3. Serialize to XML
String xml = UBL21Marshaller.invoice()
        .setFormattedOutput(true)
        .getAsString(invoice);
```

See [`EhfInvoiceGeneratorTest.java`](src/test/java/com/ehf/builder/EhfInvoiceGeneratorTest.java) for a complete working example with all fields populated.

---

## Norwegian VAT Code Mapping

The `NorwegianVatMapper` translates internal Norwegian accounting system codes to PEPPOL-compliant UNCL5305 category codes.

| Norwegian MVA-kode | PEPPOL Category | Description |
|---|---|---|
| `3`, `3U`, `31`, `31U`, `32`, `33`, `33U` | `S` | Standard rate (25%, 15%, 12%) |
| `5`, `5U` | `Z` | Zero rated goods (books, used cars, etc.) |
| `51` | `AE` | Reverse charge (domestic construction B2B) |
| `52` | `G` | Free export item (outside Norway) |
| `6` | `E` | Exempt from tax (healthcare, education, etc.) |
| `0`, `7` | `O` | Outside scope of VAT entirely |

**Important:** An unknown VAT code throws `IllegalArgumentException` immediately. This is intentional — silently defaulting to `O` or `S` on an unknown code would produce a legally non-compliant document.

### VAT Category Behavior Notes

- **`S` (Standard rate):** Must be accompanied by the explicit percentage (`25.00`, `15.00`, or `12.00`). The percentage is part of the key for BR-CO-17 grouping.
- **`Z` vs `E` vs `O`:** All three have a 0% rate in the XML, but they **cannot be merged**. They represent legally distinct tax treatments. Merging them will cause a fatal Schematron error.
- **`AE` (Reverse Charge):** The tax amount in the subtotal must be `0.00`. The buyer calculates and reports the VAT themselves.

---

## Key DTO Fields

### Document Header

| Field | Mandatory | Notes |
|---|---|---|
| `invoiceNo` | ✅ | Unique invoice identifier (BR-02) |
| `invoiceDate` | ✅ | Issue date (BR-03) |
| `invoiceDueDate` | ✅ | Payment due date |
| `currency` | ✅ | ISO 4217 (e.g., `NOK`, `EUR`) |
| `buyerReference` | ⚠️ Strongly recommended | Required for all Norwegian B2G. Missing = likely rejection |
| `invoiceType` | ⚠️ | Currently not used — defaults to `380` (Commercial Invoice). See [Known Limitations](#known-limitations) |

### Supplier (`AccountingSupplierParty`)

| Field | Notes |
|---|---|
| `companyOrganizationNo` | Norwegian Org.nr (9 digits). Used for PEPPOL EndpointID routing |
| `isVatRegistered` | If `true`, formats tax ID as `NO{OrgNr}MVA` per Norwegian requirements |
| `companyLegalName` | Official name from Foretaksregisteret |

### Invoice Line Items

| Field | Notes |
|---|---|
| `vatCode` | **Required.** Internal Norwegian code used by `NorwegianVatMapper` |
| `vatRate` | Percentage value (e.g., `25.00`, `0.00`) |
| `unitCode` | UN/ECE Rec 20 code. Examples: `EA` (each), `HUR` (hour), `KGM` (kg) |
| `amount` | Net line amount. Must equal `(price × qty) − discAmount` per BR-LIN-04 |
| `disc` | Discount percentage (e.g., `10.00` for 10%). Maps to `MultiplierFactorNumeric` |
| `type` | Only lines with `type = 1` are included in the generated XML |

### VAT Breakdown (`vatDetails`)

The `List<InvoiceVat>` must contain **one entry per unique combination** of VAT category and rate. This is required by PEPPOL business rule BR-CO-17.

```java
// Correct: Two separate entries for two different categories
InvoiceVat standard = new InvoiceVat();
standard.setVatCode("3");
standard.setVatRate(new BigDecimal("25.00"));
standard.setBaseAmount(new BigDecimal("1000.00"));
standard.setVatAmount(new BigDecimal("250.00"));

InvoiceVat exempt = new InvoiceVat();
exempt.setVatCode("6");       // E category
exempt.setVatRate(BigDecimal.ZERO);
exempt.setBaseAmount(new BigDecimal("500.00"));
exempt.setVatAmount(BigDecimal.ZERO);

// Do NOT merge these even though both have a 0% rate
```

---

## Payment Means

The builder generates up to **two** `PaymentMeans` nodes:

1. **Domestic (always included):** Local bank account (`bankAccountNo`) + KID (`kid`) for OCR reconciliation.
2. **International (when both IBAN and BIC are provided):** Separate `PaymentMeans` node with `bankAccountNoIban` and `bankAccountNoBic`.

---

## Attachments

Binary attachments (e.g., a PDF visual of the invoice) can be passed as `List<DocumentReferenceType>`:

```java
DocumentReferenceType ref = new DocumentReferenceType();
// ... set ID, description, and embedded PDF bytes
List<DocumentReferenceType> attachments = List.of(ref);

InvoiceType invoice = factory.createInvoice(dto, attachments);
```

---

## Known Limitations

The following features are not yet implemented and are candidates for future development:

- **Credit Note (type 381):** `dto.invoiceType` field exists but is currently ignored — the factory always outputs `380` (Commercial Invoice). PRs welcome.
- **Customer VAT registration check:** The customer tax scheme always formats as `NO{OrgNr}MVA` regardless of whether the customer is actually VAT-registered. This may cause issues for foreign customers or non-VAT-registered buyers.
- **Document-level allowances/charges:** The `LegalMonetaryTotal` assumes `LineExtensionAmount == TaxExclusiveAmount`. Header-level discounts or charges are not yet supported.
- **`nextInvoiceDate` field:** Defined in the DTO but not mapped to `InvoicePeriod` in the XML output.

---

## Project Structure

```
src/
└── main/java/com/ehf/builder/
    ├── EhfInvoiceFactory.java       # Core builder — orchestrates the full UBL object graph
    ├── EhfInvoiceDTO.java           # Input data transfer object
    ├── EhfInvoiceLineItem.java      # Single invoice line DTO
    ├── InvoiceVat.java              # VAT subtotal / TaxSubtotal DTO
    ├── NorwegianVatMapper.java      # MVA-kode → UNCL5305 category mapping
    └── VatCategoryEnum.java         # PEPPOL tax category codes (S, Z, E, AE, G, O, ...)
```

---

## References

- [PEPPOL BIS Billing 3.0 Specification](https://docs.peppol.eu/poacc/billing/3.0/)
- [EHF Billing 3.0 (Norwegian extension)](https://anskaffelser.dev/postaward/g3/spec/current/billing-3.0/)
- [EN 16931 (European e-invoicing standard)](https://ec.europa.eu/digital-building-blocks/sites/display/DIGITAL/EN+16931)
- [PEPPOL Code Lists](https://docs.peppol.eu/poacc/billing/3.0/codelist/)
- [UN/ECE Recommendation 20 — Unit of Measure Codes](https://unece.org/trade/uncefact/cl-recommendations)
- [ph-ubl library](https://github.com/phax/ph-ubl)

---

## License

MIT
