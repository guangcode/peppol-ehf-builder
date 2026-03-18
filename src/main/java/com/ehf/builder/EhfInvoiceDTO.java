package com.ehf.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object (DTO) for EHF 3.0 / PEPPOL BIS Billing 3.0 Invoice.
 * This core payload object maps internal business models to the standardized
 * UBL (Universal Business Language) XML semantic model.
 * * @author Gus Hu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhfInvoiceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ================= 1. Basic Header Information =================

    /**
     * Invoice type code (e.g., 380 for Commercial Invoice, 381 for Credit Note).
     * Ref: UNCL1001
     */
    private Integer invoiceType;

    /**
     * Unique invoice number / identifier assigned by the supplier.
     */
    private String invoiceNo;

    /**
     * Date when the invoice was issued.
     */
    private LocalDate invoiceDate;

    /**
     * Date when the payment is due.
     */
    private LocalDate invoiceDueDate;

    /**
     * Order reference number provided by the buyer.
     */
    private String orderNo;

    /**
     * Free-text note or terms on the invoice level (UBL: Note).
     */
    private String invoiceText;

    /**
     * Buyer reference (e.g., PO number, project code, or contact person code).
     * Highly critical for Norwegian B2G (Business-to-Government) routing.
     */
    private String buyerReference;

    // ================= 2. Supplier Details (AccountingSupplierParty) =================

    /**
     * Supplier's organization number (Norwegian Org.nr, usually 9 digits).
     */
    private String companyOrganizationNo;

    /**
     * Supplier's trading name or display name.
     */
    private String companyName;

    /**
     * Supplier's official legal name registered in the national registry
     * (e.g., Foretaksregisteret in Norway).
     */
    private String companyLegalName;

    private String companyAddress;
    private String companyPostcode;
    private String companyTown;

    /**
     * ISO 3166-1 alpha-2 country code (e.g., "NO" for Norway).
     */
    private String companyCountryCode;

    private String companyPhone;
    private String companyEmail;

    /**
     * Name of the supplier's contact person.
     */
    private String contactPerson;

    /**
     * Indicates whether the supplier is registered in the Value Added Tax (VAT) register.
     * If true, the "MVA" suffix is typically appended to the company ID in Norway.
     */
    private Boolean supplierIsVatRegistered;

    // ================= 3. Customer Details (AccountingCustomerParty) =================

    /**
     * Customer's organization number used for PEPPOL endpoint routing.
     */
    private String customerOrgCode;
    private Boolean customerIsVatRegistered;
    private String customerName;
    private String customerAddress;
    private String customerPostCode;
    private String customerTown;
    private String customerCountryCode;

    // ================= 4. Delivery Details =================

    /**
     * Actual date of delivery for the goods or services.
     */
    private LocalDate deliveryDate;
    private String deliveryAddress;
    private String deliveryPostcode;
    private String deliveryTown;
    private String deliveryCountryCode;

    // ================= 5. Payment Means =================

    /**
     * Norwegian Customer Identification Number (Kundeidentifikasjonsnummer / KID).
     * Used for OCR (Optical Character Recognition) payment matching in Nordic banking.
     */
    private String kid;

    /**
     * Local domestic bank account number (usually 11 digits in Norway).
     */
    private String bankAccountNo;

    /**
     * International Bank Account Number (IBAN).
     */
    private String bankAccountNoIban;

    /**
     * Bank Identifier Code (BIC) / SWIFT code for the corresponding IBAN.
     */
    private String bankAccountNoBic;

    // ================= 6. Monetary & Tax Totals =================

    /**
     * Document currency code (ISO 4217, e.g., "NOK", "EUR").
     */
    private String currency;

    /**
     * Total amount exclusive of VAT (Line extension amount).
     */
    private BigDecimal netAmount;

    /**
     * Total VAT amount for the entire invoice.
     */
    private BigDecimal vatAmount;

    /**
     * Total amount inclusive of VAT (Tax inclusive amount).
     */
    private BigDecimal realTotalAmount;

    /**
     * Amount to be added or subtracted to round the total payable amount.
     */
    private BigDecimal rounding;

    /**
     * Total payable amount (Total inclusive VAT + Rounding).
     */
    private BigDecimal totalAmount;

    /**
     * Breakdown of tax subtotals grouped by VAT category and VAT rate.
     * Crucial for satisfying PEPPOL Business Rule BR-CO-17.
     */
    private List<InvoiceVat> vatDetails;

    // ================= 7. Invoice Line Items =================

    /**
     * Collection of individual invoice lines.
     */
    private List<EhfInvoiceLineItem> items;
}