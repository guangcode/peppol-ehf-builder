package com.ehf.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping Norwegian internal VAT codes (MVA-koder) 
 * to the standardized PEPPOL / UNCL5305 Tax Category Codes.
 * <p>
 * This mapper acts as the crucial bridge between local Norwegian tax accounting 
 * (e.g., codes 3, 5, 6, 31) and the European e-invoicing standard EN 16931 
 * (codes S, Z, E, AE, etc.).
 * </p>
 * * @author Gus Hu.
 * @date 2024-05
 */
public class NorwegianVatMapper {

    /**
     * Cache for the mapping relationship between Norwegian system VAT codes 
     * and the corresponding PEPPOL VAT Category Enum.
     */
    private static final Map<String, VatCategoryEnum> VAT_CODE_MAP = new HashMap<>();

    static {
        // S: Standard rate (Must be used in conjunction with specific tax percentages like 25.00, 15.00, 12.00, 11.11)
        // Includes standard, reduced, and raw fish (råfisk) rates.
        VAT_CODE_MAP.put("3", VatCategoryEnum.S);
        VAT_CODE_MAP.put("3U", VatCategoryEnum.S);
        VAT_CODE_MAP.put("31", VatCategoryEnum.S);
        VAT_CODE_MAP.put("31U", VatCategoryEnum.S);
        VAT_CODE_MAP.put("32", VatCategoryEnum.S);
        VAT_CODE_MAP.put("33", VatCategoryEnum.S);
        VAT_CODE_MAP.put("33U", VatCategoryEnum.S);

        // Z: Zero rated goods (Fritatt for MVA - VAT exempt but within tax scope, e.g., books, used cars)
        VAT_CODE_MAP.put("5", VatCategoryEnum.Z);
        VAT_CODE_MAP.put("5U", VatCategoryEnum.Z);

        // AE: Reverse charge (Omvendt avgiftsplikt - Typically for domestic construction B2B)
        VAT_CODE_MAP.put("51", VatCategoryEnum.AE);

        // G: Free export item (Utførsel av varer/tjenester - Export outside Norway)
        VAT_CODE_MAP.put("52", VatCategoryEnum.G);

        // E: Exempt from tax (Unntatt fra MVA - Outside tax scope, no deduction right, e.g., healthcare, education)
        VAT_CODE_MAP.put("6", VatCategoryEnum.E);

        // O: Outside scope (Ingen mvabehandling - Pure financial transactions, completely unrelated to VAT)
        VAT_CODE_MAP.put("0", VatCategoryEnum.O);
        VAT_CODE_MAP.put("7", VatCategoryEnum.O);
    }

    /**
     * Retrieves the corresponding PEPPOL Tax Category (UNCL5305) based on the Norwegian VAT code.
     * * @param vatCode The internal Norwegian VAT code (e.g., "31", "5", "6").
     * @return The strictly mapped {@link VatCategoryEnum} for EHF XML generation.
     * @throws IllegalArgumentException If the provided vatCode is unknown or null. 
     * This fail-fast mechanism is a deliberate design choice to prevent 
     * generating legally non-compliant invoices (e.g., silently defaulting to 'O' or 'S') 
     * which could lead to audit failures or rejection by the PEPPOL network.
     */
    public static VatCategoryEnum getCategoryByVatCode(String vatCode) {
        if (vatCode == null || !VAT_CODE_MAP.containsKey(vatCode)) {
            // Fallback Strategy: Throwing an exception is highly recommended here over a silent default.
            // It forces the upstream system to investigate dirty data before transmitting tax-sensitive documents.
            throw new IllegalArgumentException("Unknown or unsupported Norwegian VAT code provided: " + vatCode);
        }
        return VAT_CODE_MAP.get(vatCode);
    }
}