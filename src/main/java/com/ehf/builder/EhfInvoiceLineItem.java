package com.ehf.builder;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) representing a single invoice line item.
 * Maps to the UBL `<cac:InvoiceLine>` element in PEPPOL BIS Billing 3.0.
 * * @author Gus Hu.
 */
@Data
public class EhfInvoiceLineItem {

    /**
     * Item description or product name.
     * Mapped to UBL: {@code cac:Item/cbc:Name}
     */
    private String description;

    /**
     * Quantity of the item invoiced.
     * Mapped to UBL: {@code cbc:InvoicedQuantity}
     */
    private BigDecimal qty;

    /**
     * Human-readable unit of measure (e.g., "pcs", "kg", "hours").
     * Note: This is typically for UI display; the XML strictly requires {@code unitCode}.
     */
    private String unit;

    /**
     * Standardized Unit of Measure code based on UN/ECE Recommendation 20.
     * Examples: "H87" (Piece), "KGM" (Kilogram), "HUR" (Hour).
     * Mapped to UBL: {@code cbc:InvoicedQuantity/@unitCode}
     * Must not be null or empty.
     */
    private String unitCode;

    /**
     * Net price of the item per unit, exclusive of VAT.
     * Mapped to UBL: {@code cac:Price/cbc:PriceAmount}
     */
    private BigDecimal price;

    /**
     * Discount percentage or multiplier applied to the base price (e.g., 10.00 for 10%).
     * Mapped to UBL: {@code cac:AllowanceCharge/cbc:MultiplierFactorNumeric}
     */
    private BigDecimal disc;

    /**
     * Total discount amount applied to this specific line.
     * Mapped to UBL: {@code cac:AllowanceCharge/cbc:Amount}
     */
    private BigDecimal discAmount;

    /**
     * Calculated VAT amount for this line (Optional for EHF line level, 
     * but useful for internal business logic).
     */
    private BigDecimal vat;

    /**
     * Line extension amount (Net amount).
     * WARNING: To comply with PEPPOL Business Rule BR-LIN-04, 
     * this MUST equal: (price * qty) - discAmount + lineCharges.
     * Mapped to UBL: {@code cbc:LineExtensionAmount}
     */
    private BigDecimal amount;

    /**
     * Seller's internal item identification or SKU.
     * Mapped to UBL: {@code cac:Item/cac:SellersItemIdentification/cbc:ID}
     */
    private String code;

    /**
     * Internal business type identifier used for payload filtering.
     * (e.g., 1 for standard product lines, other values might indicate text-only lines).
     */
    private Integer type; 

    /**
     * The VAT rate percentage applied to this item (e.g., 25.00, 15.00, 0.00).
     * Mapped to UBL: {@code cac:Item/cac:ClassifiedTaxCategory/cbc:Percent}
     */
    private BigDecimal vatRate;

    /**
     * Internal Norwegian system VAT code (e.g., "31", "5", "6").
     * Crucial for the {@code NorwegianVatMapper} to determine the exact PEPPOL 
     * VAT category (S, Z, E, AE, etc.) rather than guessing based solely on the vatRate.
     */
    private String vatCode;
}