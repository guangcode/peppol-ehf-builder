# PEPPOL EHF 3.0 Invoice Builder (Java)

![Java](https://img.shields.io/badge/Java-11%2B-blue.svg)
![PEPPOL](https://img.shields.io/badge/PEPPOL-BIS%20Billing%203.0-success.svg)
![EHF](https://img.shields.io/badge/EHF-3.0-orange.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A lightweight, high-performance, and strictly compliant Java builder for **PEPPOL BIS Billing 3.0** and **Norwegian EHF 3.0** e-invoicing. 

Designed for enterprise B2B/B2G integrations, this library bridges the gap between local Norwegian accounting systems (e.g., Tripletex, PowerOffice, Fiken) and the complex European EN 16931 XML standard.

## 🚀 Why This Project?

Building UBL (Universal Business Language) XMLs for PEPPOL from scratch is notoriously painful. Most existing libraries either force you into heavy dependency injection frameworks or leave you guessing how to map local tax codes to international standards.

This project solves the hardest parts of e-invoicing:

* **Zero Framework Lock-in**: Uses a pure object-instantiation pattern. It generates standard `InvoiceType` objects that are 100% compatible with serializers like `ph-ubl`, without forcing you into Guice or Spring Boot conflicts.
* **Built-in Norwegian Tax Compliance**: Features the `NorwegianVatMapper`, which automatically translates local Norwegian MVA-koder (e.g., 3, 5, 6, 31) into strictly validated UNCL5305 tax categories (S, Z, E, AE, O, G).
* **Strict EN 16931 Validation**: Architected to prevent fatal Schematron errors. It inherently enforces complex business rules like **BR-CO-17** (Tax Subtotal grouping) and **BR-LIN-04** (Line extension amount calculations).
* **Nordic B2G Ready**: Native support for critical Nordic routing fields like `KID` (Customer Identification Number for OCR) and `BuyerReference`.

## 📦 Core Architecture

1. **`EhfInvoiceDTO`**: The normalized payload object. Feed your upstream business data into this clean DTO.
2. **`NorwegianVatMapper`**: The tax compliance engine. Prevents illegal VAT combinations before the XML is even generated.
3. **`EhfInvoiceFactory`**: The core builder. Translates the DTO into a deeply nested, schema-valid UBL 2.1 Object tree.

## 🛠️ Quick Start

Here is a minimal example of how to generate a compliant EHF 3.0 XML invoice:

```java
import com.ehf.builder.*;
import com.helger.ubl21.UBL21Marshaller;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceDemo {
    public static void main(String[] args) {
        // 1. Populate the business DTO
        EhfInvoiceDTO dto = new EhfInvoiceDTO();
        dto.setInvoiceNo("INV-2024-001");
        dto.setInvoiceDate(LocalDate.now());
        dto.setCurrency("NOK");
        dto.setBuyerReference("Dept-42"); // Crucial for B2G
        
        // ... (Populate Supplier, Customer, and Payment details) ...

        // 2. Add Tax Totals (Automatically grouped & mapped internally)
        InvoiceVat vat = new InvoiceVat();
        vat.setBaseAmount(new BigDecimal("100.00"));
        vat.setVatAmount(new BigDecimal("25.00"));
        vat.setVatRate(new BigDecimal("25"));
        vat.setVatCode("3"); // Internal Norwegian Code -> Maps to 'S'
        dto.setVatDetails(List.of(vat));

        // 3. Generate the UBL Object
        EhfInvoiceFactory factory = new EhfInvoiceFactory();
        InvoiceType invoice = factory.createInvoice(dto, null);

        // 4. Serialize to XML (using ph-ubl)
        String xmlPayload = UBL21Marshaller.invoice()
                .setFormattedOutput(true)
                .getAsString(invoice);

        System.out.println(xmlPayload);
    }
}
```

*(For a complete, robust example including line-level discounts and PDF attachment embedding, check the `EhfInvoiceGeneratorTest.java` in the source code).*

## 🛡️ Business Rules (Schematron) Handled

This library proactively designs around the most common reasons PEPPOL Access Points reject invoices:

* **BR-CO-17**: Ensures tax subtotals are strictly aggregated by VAT category and rate.
* **BR-LIN-04**: Validates that invoice line net amounts strictly match the `(Price * Qty) - Discount` formula.
* **BR-CO-26**: Injects the mandatory `Foretaksregisteret` scheme for Norwegian AS/ASA entities.

## 👨‍💻 About the Author

**Gus Hu** ([@guangcode](https://github.com/guangcode))  
12+ YOE Java Architect | Deep expertise in Enterprise Architecture, Netty & Complex B2B Integrations.  

*Open to global remote opportunities and cross-border tech collaborations.*

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
