package com.ehf.builder;

import com.helger.ubl21.UBL21Marshaller;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DocumentDescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.EmbeddedDocumentBinaryObjectType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration Test & Example Usage for the EHF 3.0 Invoice Builder.
 * <p>
 * This class demonstrates how to populate the {@link EhfInvoiceDTO} with standard
 * Norwegian business data, generate the PEPPOL-compliant UBL object, and serialize
 * it into a formatted XML string using the ph-ubl library.
 * </p>
 *
 * @author Gus Hu (https://github.com/guangcode)
 * @date 2024-05
 */
public class EhfInvoiceGeneratorTest {

    public static void main(String[] args) throws Exception {

        // =========================================================================
        // Step 1: Initialize and populate the DTO with upstream business data.
        // =========================================================================
        EhfInvoiceDTO dto = new EhfInvoiceDTO();

        // --- Core Document Header ---
        dto.setInvoiceNo("13083");
        dto.setInvoiceDate(LocalDate.parse("2023-08-14"));
        dto.setInvoiceDueDate(LocalDate.parse("2023-09-07"));
        dto.setCurrency("NOK"); // Norwegian Krone
        dto.setInvoiceText("1123");

        // BuyerReference is mandatory for Norwegian B2G and most B2B transactions.
        dto.setBuyerReference("Morten Tveten");
        dto.setOrderNo("1123");

        // --- Supplier Information (AccountingSupplierParty) ---
        // Norwegian Organization Numbers are strictly 9 digits.
        dto.setCompanyOrganizationNo("336880697");
        dto.setCompanyName("Test, Autorisert regnskapsførerselskap / Godkjent revisjonsselskap");
        dto.setCompanyAddress("Test 2");
        dto.setCompanyTown("Test");
        dto.setCompanyPostcode("2069");
        dto.setCompanyCountryCode("NO");

        // Triggers the "NO...MVA" tax scheme logic in the builder
        dto.setSupplierIsVatRegistered(true);
        dto.setCompanyLegalName("Test");
        dto.setContactPerson("Test");
        dto.setCompanyPhone("91175475");
        dto.setCompanyEmail("test@gmai.com");

        // --- Customer Information (AccountingCustomerParty) ---
        dto.setCustomerOrgCode("881086591");
        dto.setCustomerIsVatRegistered(true);
        dto.setCustomerName("Test AS");
        dto.setCustomerAddress("Test 11A");
        dto.setCustomerTown("Test");
        dto.setCustomerPostCode("2068");
        dto.setCustomerCountryCode("NO");

        // --- Delivery Information ---
        dto.setDeliveryDate(LocalDate.parse("2023-08-14"));
        dto.setDeliveryAddress("Test 11A");
        dto.setDeliveryTown("Test");
        dto.setDeliveryPostcode("2068");
        dto.setDeliveryCountryCode("NO");

        // --- Payment Means ---
        // KID (Kundeidentifikasjonsnummer) is critical for OCR-based automated bank reconciliation in Norway.
        dto.setKid("01010300130836");
        dto.setBankAccountNo("18003195402");
        dto.setBankAccountNoIban("NO1818003195402");
        dto.setBankAccountNoBic("DNBANOKKXXX");

        // --- Document Level Tax Totals ---
        // Demonstrating BR-CO-17 compliance: Grouping tax subtotals by VAT code/rate.
        dto.setVatAmount(new BigDecimal("25.75"));
        List<InvoiceVat> vatDetails = new ArrayList<>();

        // Tax Subtotal 1: Standard Rate (25%) using internal code '3'
        InvoiceVat vat1 = new InvoiceVat();
        vat1.setBaseAmount(new BigDecimal("103.00"));
        vat1.setVatAmount(new BigDecimal("25.75"));
        vat1.setVatRate(new BigDecimal("25"));
        vat1.setVatCode("3");
        vatDetails.add(vat1);

        // Tax Subtotal 2: Outside Scope / No Tax (0%) using internal code '0'
        InvoiceVat vat2 = new InvoiceVat();
        vat2.setBaseAmount(new BigDecimal("5.00"));
        vat2.setVatAmount(new BigDecimal("0.00"));
        vat2.setVatRate(new BigDecimal("0"));
        vat2.setVatCode("0");
        vatDetails.add(vat2);

        dto.setVatDetails(vatDetails);

        // --- Document Level Monetary Totals ---
        dto.setNetAmount(new BigDecimal("108.00"));
        dto.setRealTotalAmount(new BigDecimal("133.75"));
        dto.setRounding(new BigDecimal("0.25"));
        dto.setTotalAmount(new BigDecimal("134.00"));

        // --- Invoice Lines ---
        List<EhfInvoiceLineItem> items = new ArrayList<>();

        // Line 1: Standard item with a discount applied.
        // UN/ECE Unit Code "EA" stands for 'Each' (pieces).
        EhfInvoiceLineItem line1 = new EhfInvoiceLineItem();
        line1.setType(1);
        line1.setQty(new BigDecimal("1.0000"));
        line1.setUnitCode("EA");
        line1.setAmount(new BigDecimal("90.00")); // Net amount after discount
        line1.setPrice(new BigDecimal("100.0000"));
        line1.setDisc(new BigDecimal("10.0000"));
        line1.setDiscAmount(new BigDecimal("10.00"));
        line1.setDescription("534r");
        line1.setCode("17");
        line1.setVat(new BigDecimal("25.00"));
        line1.setVatRate(new BigDecimal("25"));
        line1.setVatCode("3");
        items.add(line1);

        // Line 2: Zero-rated/Outside scope item
        EhfInvoiceLineItem line2 = new EhfInvoiceLineItem();
        line2.setType(1);
        line2.setQty(new BigDecimal("1.0000"));
        line2.setUnitCode("EA");
        line2.setAmount(new BigDecimal("5.00"));
        line2.setPrice(new BigDecimal("5.0000"));
        line2.setDescription("Test 81");
        line2.setCode("Test 81");
        line2.setVat(BigDecimal.ZERO);
        line2.setVatRate(BigDecimal.ZERO);
        line2.setVatCode("0");
        items.add(line2);

        // Line 3: Edge case - Zero quantity and zero amount (e.g., info line mapped as zero-rated)
        EhfInvoiceLineItem line3 = new EhfInvoiceLineItem();
        line3.setType(1);
        line3.setQty(new BigDecimal("0.0000"));
        line3.setUnitCode("EA");
        line3.setAmount(BigDecimal.ZERO);
        line3.setPrice(BigDecimal.ZERO);
        line3.setDescription("NA");
        line3.setCode("NA");
        line3.setVat(BigDecimal.ZERO);
        line3.setVatRate(BigDecimal.ZERO);
        line3.setVatCode("5"); // Code '5' maps to 'Z' (Zero rated goods)
        items.add(line3);

        // Line 4: Service item billed by the hour.
        // UN/ECE Unit Code "HUR" stands for 'Hour'.
        EhfInvoiceLineItem line4 = new EhfInvoiceLineItem();
        line4.setType(1);
        line4.setQty(new BigDecimal("1.0000"));
        line4.setUnitCode("HUR");
        line4.setAmount(new BigDecimal("13.00"));
        line4.setPrice(new BigDecimal("13.0000"));
        line4.setDescription("534r");
        line4.setCode("17");
        line4.setVat(new BigDecimal("25.00"));
        line4.setVatRate(new BigDecimal("25"));
        line4.setVatCode("3U");
        items.add(line4);

        dto.setItems(items);

        // =========================================================================
        // Step 2: Construct binary attachments (e.g., embedding the visual PDF)
        // =========================================================================
        List<DocumentReferenceType> attachments = new ArrayList<>();
        DocumentReferenceType addDocRef = new DocumentReferenceType();
        IDType docId = new IDType();
        docId.setValue("13083.pdf");
        addDocRef.setID(docId);

        DocumentDescriptionType docDesc = new DocumentDescriptionType();
        docDesc.setValue("Commercial invoice");
        addDocRef.setDocumentDescription(List.of(docDesc));

        AttachmentType attachment = new AttachmentType();
        EmbeddedDocumentBinaryObjectType binObj = new EmbeddedDocumentBinaryObjectType();
        binObj.setMimeCode("application/pdf");
        binObj.setFilename("13083.pdf");
        // In a real scenario, this would be the actual Base64 encoded byte array of the file.
        binObj.setValue("JVBERi0xLjUNCiWDkvr...".getBytes());
        attachment.setEmbeddedDocumentBinaryObject(binObj);
        addDocRef.setAttachment(attachment);
        attachments.add(addDocRef);

        // =========================================================================
        // Step 3: Invoke the Factory to generate the UBL Object model
        // =========================================================================
        EhfInvoiceFactory factory = new EhfInvoiceFactory();
        InvoiceType invoice = factory.createInvoice(dto, attachments);

        // Optional post-processing:
        // Manually injecting a ContractDocumentReference if not covered by the standard DTO.
        // This demonstrates the flexibility of manipulating the UBL object post-generation.
        DocumentReferenceType contractRef = new DocumentReferenceType();
        IDType contractId = new IDType();
        contractId.setValue("4124");
        contractRef.setID(contractId);
        invoice.addContractDocumentReference(contractRef);

        // =========================================================================
        // Step 4: Marshall the Java Object into an EHF 3.0 / PEPPOL compliant XML
        // =========================================================================
        String xmlOutput = UBL21Marshaller.invoice()
                .setFormattedOutput(true)
                .getAsString(invoice);

        System.out.println("====== Generated EHF 3.0 XML Payload ======");
        System.out.println(xmlOutput);
    }
}