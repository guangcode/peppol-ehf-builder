package com.ehf.builder;

import lombok.Getter;

/**
 * Enum representing the UNCL5305 Duty or Tax or Fee Category Codes.
 * <p>
 * This standard enumeration is used in PEPPOL BIS Billing 3.0 (and EHF 3.0) 
 * to strictly classify the VAT nature of an invoice line or a tax subtotal.
 * </p>
 * * @author Gus Hu.
 * @date 2024-05
 */
@Getter
public enum VatCategoryEnum {

    /**
     * VAT Reverse Charge.
     * Used when the buyer is liable for the tax (e.g., domestic construction services).
     */
    AE("AE", "Vat Reverse Charge"),

    /**
     * Exempt from Tax.
     * Used for goods or services that are legally exempt from VAT (e.g., healthcare, education).
     * The seller cannot deduct incoming VAT.
     */
    E("E", "Exempt from Tax"),

    /**
     * Standard rate.
     * The standard, reduced, or specific VAT rate applies.
     * Must be accompanied by the exact percentage (e.g., 25.00, 15.00).
     */
    S("S", "Standard rate"),

    /**
     * Zero rated goods.
     * Goods/services within the tax scope but currently taxed at 0% 
     * (e.g., books, used cars in Norway). Seller retains deduction rights.
     */
    Z("Z", "Zero rated goods"),

    /**
     * Free export item, VAT not charged.
     * Used for goods or services exported outside the local VAT jurisdiction.
     */
    G("G", "Free export item, VAT not charged"),

    /**
     * Services outside scope of tax.
     * Used for transactions that are completely outside the VAT legislation 
     * (e.g., financial transfers, fines, compensation).
     */
    O("O", "Services outside scope of tax"),

    /**
     * VAT exempt for EEA intra-community supply of goods and services.
     */
    K("K", "VAT exempt for EEA intra-community supply of goods and services"),

    /**
     * Canary Islands general indirect tax.
     */
    L("L", "Canary Islands general indirect tax"),

    /**
     * Tax for production, services and importation in Ceuta and Melilla.
     */
    M("M", "Tax for production, services and importation in Ceuta and Melilla"),

    /**
     * Transferred (VAT), In Italy.
     */
    B("B", "Transferred (VAT), In Italy");

    private final String code;
    private final String description;

    VatCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}