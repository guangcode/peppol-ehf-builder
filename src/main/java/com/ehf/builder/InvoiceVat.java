package com.ehf.builder;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) representing the VAT breakdown / Tax Subtotal.
 * Mapped to the UBL {@code <cac:TaxSubtotal>} element in PEPPOL BIS Billing 3.0.
 * <p>
 * WARNING: To comply with PEPPOL Business Rule BR-CO-17, the invoice tax totals 
 * MUST be grouped by the combination of VAT Category and VAT Rate. 
 * Do not merge different tax categories even if their tax rates are both 0.00.
 * </p>
 * * @Author Gus Hu.
 * @Date 2024-05
 **/
@Data
public class InvoiceVat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The net amount subject to this specific tax category and tax rate.
     * Mapped to UBL: {@code cac:TaxSubtotal/cbc:TaxableAmount}
     */
    private BigDecimal baseAmount;

    /**
     * The calculated VAT amount for this specific tax category and tax rate.
     * Mapped to UBL: {@code cac:TaxSubtotal/cbc:TaxAmount}
     */
    private BigDecimal vatAmount;

    /**
     * The VAT rate percentage (e.g., 25.00 for Standard, 15.00 for Reduced, 0.00 for Zero/Exempt).
     * Mapped to UBL: {@code cac:TaxSubtotal/cac:TaxCategory/cbc:Percent}
     */
    private BigDecimal vatRate;

    /**
     * Internal Norwegian system VAT code (MVA-kode, e.g., "3", "5", "6", "31").
     * <p>
     * CRITICAL: This field is used by the {@code NorwegianVatMapper} to accurately 
     * determine the UN/CEFACT tax category code (S, Z, E, AE, O, G). 
     * It ensures compliance with Norwegian tax laws (MVA-loven) during B2B/B2G data transmission.
     * </p>
     */
    private String vatCode;

}