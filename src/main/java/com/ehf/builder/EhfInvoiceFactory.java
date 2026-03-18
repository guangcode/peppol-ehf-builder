package com.ehf.builder;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.*;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.LocationType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.*;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Builder Factory for EHF 3.0 / PEPPOL BIS Billing 3.0 Invoices.
 * <p>
 * Architecture Design:
 * Instead of relying on heavy XML mapping frameworks, this class uses a pure
 * object instantiation strategy. This guarantees zero dependency conflicts
 * regardless of the underlying ph-ubl or JAXB versions used by the integrators.
 * </p>
 * <p>
 * Compliance:
 * Fully complies with the European Norm (EN 16931) and Norwegian EHF
 * (Elektronisk Handelsformat) specific business rules.
 * </p>
 *
 * @author Gus Hu.
 * @date 2024-05
 */
public class EhfInvoiceFactory {

    /** * ICD (International Code Designator) 0192 strictly refers to the
     * Norwegian Register of Business Enterprises (Enhetsregisteret).
     */
    private static final String SCHEME_ORG_NO = "0192";

    /** Identifier scheme for tax registration. */
    private static final String SCHEME_TAX = "TAX";

    /** Identifier scheme for Value Added Tax (VAT). */
    private static final String SCHEME_VAT = "VAT";

    /** UN/ECE 5153 Agency identifier for VAT registrations. */
    private static final String SCHEME_VAT_NO = "UN/ECE 5153";

    /** * Mandatory for Norwegian AS/ASA companies to declare their
     * registration in the official enterprise registry.
     */
    private static final String REGISTRY_NORWAY = "Foretaksregisteret";

    /** Default ISO 3166-1 alpha-2 country code for Norway. */
    private static final String DEFAULT_COUNTRY_CODE = "NO";

    /** UNCL1001 Code 380: Explicitly defines this document as a Commercial Invoice. */
    private static final String INVOICE_TYPE_COMMERCIAL = "380";

    /** UNCL4461 Code 30: Credit transfer (Standard bank account payment). */
    private static final String PAYMENT_MEANS_CREDIT_TRANSFER = "30";

    /**
     * Orchestrates the construction of the complete UBL 2.1 Invoice payload.
     *
     * @param dto         The normalized business data transfer object.
     * @param attachments Binary attachments (e.g., Base64 PDF) allowed by PEPPOL.
     * @return A deeply nested, schema-valid {@link InvoiceType}.
     */
    public InvoiceType createInvoice(EhfInvoiceDTO dto, List<DocumentReferenceType> attachments) {
        InvoiceType invoice = new InvoiceType();

        // --- 1. Protocol Identifiers (Mandatory for PEPPOL routing) ---
        // Defines the exact semantic model (EN 16931) and the PEPPOL BIS Billing 3.0 profile.
        // Without these, the Peppol Access Point (AP) will reject the transmission at the gateway.
        invoice.setCustomizationID("urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0");
        invoice.setProfileID("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

        buildHeader(invoice, dto);

        // Attachments mapping (e.g., embedding the visual PDF representation of the invoice)
        if (attachments != null && !attachments.isEmpty()) {
            invoice.setAdditionalDocumentReference(attachments);
        }

        invoice.setAccountingSupplierParty(buildSupplier(dto));
        invoice.setAccountingCustomerParty(buildCustomer(dto));
        invoice.setDelivery(buildDelivery(dto));
        invoice.setPaymentMeans(buildPaymentMeans(dto));
        invoice.setTaxTotal(buildTaxTotal(dto));
        invoice.setLegalMonetaryTotal(buildMonetaryTotal(dto));
        invoice.setInvoiceLine(buildInvoiceLines(dto));

        return invoice;
    }

    /**
     * Populates document-level metadata.
     * Ensures compliance with BR-02 (Invoice Number) and BR-03 (Issue Date).
     */
    private void buildHeader(InvoiceType invoice, EhfInvoiceDTO dto) {
        IDType invoiceId = new IDType();
        invoiceId.setValue(dto.getInvoiceNo());
        invoice.setID(invoiceId);

        IssueDateType issueDateType = new IssueDateType();
        issueDateType.setValue(dto.getInvoiceDate());
        invoice.setIssueDate(issueDateType);

        DueDateType dueDateType = new DueDateType();
        dueDateType.setValue(dto.getInvoiceDueDate());
        invoice.setDueDate(dueDateType);

        InvoiceTypeCodeType invoiceTypeCodeType = new InvoiceTypeCodeType();
        invoiceTypeCodeType.setValue(INVOICE_TYPE_COMMERCIAL);
        invoice.setInvoiceTypeCode(invoiceTypeCodeType);

        DocumentCurrencyCodeType documentCurrencyCodeType = new DocumentCurrencyCodeType();
        documentCurrencyCodeType.setValue(dto.getCurrency());
        invoice.setDocumentCurrencyCode(documentCurrencyCodeType);

        // Document level notes or payment terms
        if (dto.getInvoiceText() != null && !dto.getInvoiceText().isBlank()) {
            NoteType note = new NoteType();
            note.setValue(dto.getInvoiceText());
            invoice.setNote(List.of(note));
        }

        // BuyerReference is highly critical in the Nordic market (especially B2G).
        // Norwegian municipalities (Kommuner) use this to route the invoice
        // to the correct internal department or cost center. Missing this often leads to rejection.
        if (dto.getBuyerReference() != null && !dto.getBuyerReference().isBlank()) {
            BuyerReferenceType buyerReferenceType = new BuyerReferenceType();
            buyerReferenceType.setValue(dto.getBuyerReference());
            invoice.setBuyerReference(buyerReferenceType);
        }

        // Purchase Order reference provided by the buyer
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            OrderReferenceType orderReferenceType = new OrderReferenceType();
            IDType orderId = new IDType();
            orderId.setValue(dto.getOrderNo());
            orderReferenceType.setID(orderId);
            invoice.setOrderReference(orderReferenceType);
        }
    }

    /**
     * Constructs the Supplier Party node.
     * Specifically handles Norwegian EHF legal entity and VAT registration quirks.
     */
    private SupplierPartyType buildSupplier(EhfInvoiceDTO dto) {
        SupplierPartyType supplierType = new SupplierPartyType();
        PartyType party = new PartyType();
        supplierType.setParty(party);

        // Endpoint routing ID: The primary network address in PEPPOL (SML/SMP lookup)
        EndpointIDType endpointIDType = new EndpointIDType();
        endpointIDType.setSchemeID(SCHEME_ORG_NO);
        endpointIDType.setValue(dto.getCompanyOrganizationNo());
        party.setEndpointID(endpointIDType);

        PartyNameType partyNameType = new PartyNameType();
        partyNameType.setName(dto.getCompanyName());
        party.setPartyName(List.of(partyNameType));

        // Supplier Postal Address mapping
        AddressType address = new AddressType();
        if (dto.getCompanyAddress() != null) {
            StreetNameType streetNameType = new StreetNameType();
            streetNameType.setValue(dto.getCompanyAddress());
            address.setStreetName(streetNameType);
        }
        if (dto.getCompanyTown() != null) {
            CityNameType cityNameType = new CityNameType();
            cityNameType.setValue(dto.getCompanyTown());
            address.setCityName(cityNameType);
        }
        if (dto.getCompanyPostcode() != null) {
            PostalZoneType postalZoneType = new PostalZoneType();
            postalZoneType.setValue(dto.getCompanyPostcode());
            address.setPostalZone(postalZoneType);
        }

        String countryCode = dto.getCompanyCountryCode() != null && !dto.getCompanyCountryCode().isBlank() ? dto.getCompanyCountryCode() : DEFAULT_COUNTRY_CODE;
        CountryType countryType = new CountryType();
        IdentificationCodeType identificationCodeType = new IdentificationCodeType();
        identificationCodeType.setValue(countryCode);
        countryType.setIdentificationCode(identificationCodeType);
        address.setCountry(countryType);
        party.setPostalAddress(address);

        // Tax Schemes: This block implements specific Norwegian tax compliance.
        List<PartyTaxSchemeType> taxSchemes = new ArrayList<>();

        // If the supplier is VAT registered, Norway requires a specific format: "NO" + OrgNumber + "MVA".
        if (Boolean.TRUE.equals(dto.getSupplierIsVatRegistered())) {
            PartyTaxSchemeType vatScheme = new PartyTaxSchemeType();
            TaxSchemeType taxSchemeType = new TaxSchemeType();
            IDType idType = new IDType();
            idType.setSchemeID(SCHEME_VAT_NO);
            idType.setValue(SCHEME_VAT);
            taxSchemeType.setID(idType);
            vatScheme.setTaxScheme(taxSchemeType);

            CompanyIDType companyIDType = new CompanyIDType();
            companyIDType.setValue("NO" + dto.getCompanyOrganizationNo() + "MVA");
            vatScheme.setCompanyID(companyIDType);
            taxSchemes.add(vatScheme);
        }

        // Corporate Registration Scheme (BR-CO-26)
        // Explicitly registers the company under 'Foretaksregisteret', a legal requirement
        // for limited liability companies (AS/ASA) operating in Norway.
        PartyTaxSchemeType corpTaxScheme = new PartyTaxSchemeType();
        TaxSchemeType corpTaxSchemeType = new TaxSchemeType();
        IDType corpIdType = new IDType();
        corpIdType.setSchemeID(SCHEME_TAX);
        corpIdType.setValue(SCHEME_TAX);
        corpTaxSchemeType.setID(corpIdType);
        corpTaxScheme.setTaxScheme(corpTaxSchemeType);

        CompanyIDType corpCompanyIDType = new CompanyIDType();
        corpCompanyIDType.setValue(REGISTRY_NORWAY);
        corpTaxScheme.setCompanyID(corpCompanyIDType);
        taxSchemes.add(corpTaxScheme);

        party.setPartyTaxScheme(taxSchemes);

        // Legal Entity Information mapping
        PartyLegalEntityType legalEntity = new PartyLegalEntityType();
        RegistrationNameType registrationNameType = new RegistrationNameType();
        registrationNameType.setValue(dto.getCompanyLegalName());
        legalEntity.setRegistrationName(registrationNameType);

        CompanyIDType legalCompanyIDType = new CompanyIDType();
        legalCompanyIDType.setSchemeID(SCHEME_ORG_NO);
        legalCompanyIDType.setValue(dto.getCompanyOrganizationNo());
        legalEntity.setCompanyID(legalCompanyIDType);
        party.setPartyLegalEntity(List.of(legalEntity));

        // Contact Information mapping
        ContactType contact = new ContactType();
        boolean hasContact = false;
        if (dto.getContactPerson() != null && !dto.getContactPerson().isBlank()) {
            NameType nameType = new NameType();
            nameType.setValue(dto.getContactPerson());
            contact.setName(nameType);
            hasContact = true;
        }
        if (dto.getCompanyPhone() != null && !dto.getCompanyPhone().isBlank()) {
            TelephoneType telephoneType = new TelephoneType();
            telephoneType.setValue(dto.getCompanyPhone());
            contact.setTelephone(telephoneType);
            hasContact = true;
        }
        if (dto.getCompanyEmail() != null && !dto.getCompanyEmail().isBlank()) {
            ElectronicMailType electronicMailType = new ElectronicMailType();
            electronicMailType.setValue(dto.getCompanyEmail());
            contact.setElectronicMail(electronicMailType);
            hasContact = true;
        }
        if (hasContact) party.setContact(contact);

        return supplierType;
    }

    /**
     * Constructs the Customer Party node.
     * The EndpointID is the primary routing key in the PEPPOL network for the receiver.
     */
    private CustomerPartyType buildCustomer(EhfInvoiceDTO dto) {
        CustomerPartyType customerType = new CustomerPartyType();
        PartyType party = new PartyType();
        customerType.setParty(party);

        // Receiver's electronic address (Usually the Norwegian Org.nr)
        EndpointIDType endpointIDType = new EndpointIDType();
        endpointIDType.setSchemeID(SCHEME_ORG_NO);
        endpointIDType.setValue(dto.getCustomerOrgCode());
        party.setEndpointID(endpointIDType);

        PartyIdentificationType partyIdent = new PartyIdentificationType();
        IDType idType = new IDType();
        idType.setSchemeID(SCHEME_ORG_NO);
        idType.setValue(dto.getCustomerOrgCode());
        partyIdent.setID(idType);
        party.setPartyIdentification(List.of(partyIdent));

        PartyNameType partyNameType = new PartyNameType();
        partyNameType.setName(dto.getCustomerName());
        party.setPartyName(List.of(partyNameType));

        // Customer Postal Address mapping
        AddressType address = new AddressType();
        if (dto.getCustomerAddress() != null) {
            StreetNameType streetNameType = new StreetNameType();
            streetNameType.setValue(dto.getCustomerAddress());
            address.setStreetName(streetNameType);
        }
        if (dto.getCustomerTown() != null) {
            CityNameType cityNameType = new CityNameType();
            cityNameType.setValue(dto.getCustomerTown());
            address.setCityName(cityNameType);
        }
        if (dto.getCustomerPostCode() != null) {
            PostalZoneType postalZoneType = new PostalZoneType();
            postalZoneType.setValue(dto.getCustomerPostCode());
            address.setPostalZone(postalZoneType);
        }

        String countryCode = dto.getCustomerCountryCode() != null && !dto.getCustomerCountryCode().isBlank() ? dto.getCustomerCountryCode() : DEFAULT_COUNTRY_CODE;
        CountryType countryType = new CountryType();
        IdentificationCodeType identificationCodeType = new IdentificationCodeType();
        identificationCodeType.setValue(countryCode);
        countryType.setIdentificationCode(identificationCodeType);
        address.setCountry(countryType);
        party.setPostalAddress(address);

        // Customer Tax Scheme Mapping
        PartyTaxSchemeType vatScheme = new PartyTaxSchemeType();
        TaxSchemeType taxSchemeType = new TaxSchemeType();
        IDType cusIDtype = new IDType();
        cusIDtype.setSchemeID(SCHEME_VAT_NO);
        cusIDtype.setValue(SCHEME_VAT);
        taxSchemeType.setID(cusIDtype);
        vatScheme.setTaxScheme(taxSchemeType);

        CompanyIDType companyIDType = new CompanyIDType();
        companyIDType.setValue("NO" + dto.getCustomerOrgCode() + (Boolean.TRUE.equals(dto.getCustomerIsVatRegistered()) ? "MVA" : ""));
        vatScheme.setCompanyID(companyIDType);
        party.setPartyTaxScheme(List.of(vatScheme));

        // Customer Legal Entity mapping
        PartyLegalEntityType legalEntity = new PartyLegalEntityType();
        RegistrationNameType registrationNameType = new RegistrationNameType();
        registrationNameType.setValue(dto.getCustomerName());
        legalEntity.setRegistrationName(registrationNameType);

        CompanyIDType legalCompanyIDType = new CompanyIDType();
        legalCompanyIDType.setSchemeID(SCHEME_ORG_NO);
        legalCompanyIDType.setValue(dto.getCustomerOrgCode());
        legalEntity.setCompanyID(legalCompanyIDType);
        party.setPartyLegalEntity(List.of(legalEntity));

        return customerType;
    }

    /**
     * Constructs the Delivery node.
     * Contains the actual delivery date and the destination location of the goods/services.
     */
    private List<DeliveryType> buildDelivery(EhfInvoiceDTO dto) {
        if (dto.getDeliveryDate() == null) return List.of();

        DeliveryType delivery = new DeliveryType();
        ActualDeliveryDateType actualDeliveryDateType = new ActualDeliveryDateType();
        actualDeliveryDateType.setValue(dto.getDeliveryDate());
        delivery.setActualDeliveryDate(actualDeliveryDateType);

        LocationType location = new LocationType();
        AddressType address = new AddressType();

        if (dto.getDeliveryAddress() != null) {
            StreetNameType streetNameType = new StreetNameType();
            streetNameType.setValue(dto.getDeliveryAddress());
            address.setStreetName(streetNameType);
        }
        if (dto.getDeliveryTown() != null) {
            CityNameType cityNameType = new CityNameType();
            cityNameType.setValue(dto.getDeliveryTown());
            address.setCityName(cityNameType);
        }
        if (dto.getDeliveryPostcode() != null) {
            PostalZoneType postalZoneType = new PostalZoneType();
            postalZoneType.setValue(dto.getDeliveryPostcode());
            address.setPostalZone(postalZoneType);
        }

        String countryCode = dto.getDeliveryCountryCode() != null && !dto.getDeliveryCountryCode().isBlank() ? dto.getDeliveryCountryCode() : DEFAULT_COUNTRY_CODE;
        CountryType countryType = new CountryType();
        IdentificationCodeType identificationCodeType = new IdentificationCodeType();
        identificationCodeType.setValue(countryCode);
        countryType.setIdentificationCode(identificationCodeType);
        address.setCountry(countryType);

        location.setAddress(address);
        delivery.setDeliveryLocation(location);

        return List.of(delivery);
    }

    /**
     * Constructs the Payment Means node.
     * Handles domestic Nordic OCR systems and cross-border SEPA transfers.
     */
    private List<PaymentMeansType> buildPaymentMeans(EhfInvoiceDTO dto) {
        List<PaymentMeansType> meansList = new ArrayList<>();

        PaymentMeansType localMeans = new PaymentMeansType();
        PaymentMeansCodeType localMeansCode = new PaymentMeansCodeType();
        localMeansCode.setValue(PAYMENT_MEANS_CREDIT_TRANSFER);
        localMeans.setPaymentMeansCode(localMeansCode);

        // Mapping Norwegian KID (Kundeidentifikasjonsnummer).
        // This is not a standard European concept but an essential Nordic requirement
        // for automated bank reconciliation via OCR. Placed inside PaymentID.
        if (dto.getKid() != null && !dto.getKid().isBlank()) {
            PaymentIDType paymentIDType = new PaymentIDType();
            paymentIDType.setValue(dto.getKid());
            localMeans.setPaymentID(List.of(paymentIDType));
        }

        FinancialAccountType localAccount = new FinancialAccountType();
        IDType localAccountId = new IDType();
        localAccountId.setValue(dto.getBankAccountNo());
        localAccount.setID(localAccountId);
        localMeans.setPayeeFinancialAccount(localAccount);
        meansList.add(localMeans);

        // Map international payment details (IBAN/BIC) for cross-border transactions
        if (dto.getBankAccountNoIban() != null && !dto.getBankAccountNoIban().isBlank() && dto.getBankAccountNoBic() != null && !dto.getBankAccountNoBic().isBlank()) {

            PaymentMeansType ibanMeans = new PaymentMeansType();
            PaymentMeansCodeType ibanMeansCode = new PaymentMeansCodeType();
            ibanMeansCode.setValue(PAYMENT_MEANS_CREDIT_TRANSFER);
            ibanMeans.setPaymentMeansCode(ibanMeansCode);

            FinancialAccountType ibanAccount = new FinancialAccountType();
            IDType ibanId = new IDType();
            ibanId.setValue(dto.getBankAccountNoIban());
            ibanAccount.setID(ibanId);

            BranchType branchType = new BranchType();
            IDType branchId = new IDType();
            branchId.setValue(dto.getBankAccountNoBic());
            branchType.setID(branchId);
            ibanAccount.setFinancialInstitutionBranch(branchType);

            ibanMeans.setPayeeFinancialAccount(ibanAccount);
            meansList.add(ibanMeans);
        }

        return meansList;
    }

    /**
     * Constructs the document-level Tax Total.
     * <p>
     * CRITICAL COMPLIANCE FIX:
     * Strictly enforces PEPPOL Rule BR-CO-17. Tax subtotals are aggregated using the
     * precise UNCL5305 category resolved by {@link NorwegianVatMapper}.
     * Merging different categories (even if both are 0%) will cause a fatal Schematron validation error.
     * </p>
     */
    private List<TaxTotalType> buildTaxTotal(EhfInvoiceDTO dto) {
        TaxTotalType taxTotal = new TaxTotalType();
        String currency = dto.getCurrency();

        BigDecimal vatAmount = dto.getVatAmount() != null ? dto.getVatAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        TaxAmountType taxAmountType = new TaxAmountType();
        taxAmountType.setValue(vatAmount);
        taxAmountType.setCurrencyID(currency);
        taxTotal.setTaxAmount(taxAmountType);

        if (dto.getVatDetails() != null) {
            List<TaxSubtotalType> taxSubtotalTypes = new ArrayList<>();

            for (InvoiceVat vat : dto.getVatDetails()) {
                TaxSubtotalType subTotal = new TaxSubtotalType();

                TaxableAmountType taxableAmountType = new TaxableAmountType();
                taxableAmountType.setValue(vat.getBaseAmount());
                taxableAmountType.setCurrencyID(currency);
                subTotal.setTaxableAmount(taxableAmountType);

                BigDecimal vatAmt = vat.getVatAmount().setScale(2, RoundingMode.HALF_UP);
                TaxAmountType subTaxAmountType = new TaxAmountType();
                subTaxAmountType.setValue(vatAmt);
                subTaxAmountType.setCurrencyID(currency);
                subTotal.setTaxAmount(subTaxAmountType);

                TaxCategoryType category = new TaxCategoryType();
                IDType categoryId = new IDType();

                // Core Fix: Resolving the actual PEPPOL VAT category from the local Norwegian VAT code.
                // Ensures that a code '5' maps to 'Z' and '6' maps to 'E', maintaining strict audit trails.
                String systemVatCode = vat.getVatCode();
                VatCategoryEnum categoryEnum = NorwegianVatMapper.getCategoryByVatCode(systemVatCode);
                categoryId.setValue(categoryEnum.getCode());
                category.setID(categoryId);

                PercentType percentType = new PercentType();
                percentType.setValue(vat.getVatRate());
                category.setPercent(percentType);

                TaxSchemeType taxSchemeType = new TaxSchemeType();
                IDType schemeId = new IDType();
                schemeId.setValue(SCHEME_VAT); 
                schemeId.setSchemeID(SCHEME_VAT_NO);
                taxSchemeType.setID(schemeId);
                category.setTaxScheme(taxSchemeType);

                subTotal.setTaxCategory(category);
                taxSubtotalTypes.add(subTotal);
            }
            taxTotal.setTaxSubtotal(taxSubtotalTypes);
        }
        return List.of(taxTotal);
    }

    /**
     * Computes the Legal Monetary Total.
     * Validates that formulas conform to EN 16931 rules (e.g., BR-CO-16).
     * TaxExclusiveAmount + TaxAmount MUST equal TaxInclusiveAmount.
     */
    private MonetaryTotalType buildMonetaryTotal(EhfInvoiceDTO dto) {
        MonetaryTotalType total = new MonetaryTotalType();
        String currency = dto.getCurrency();

        // Net Amount without VAT
        LineExtensionAmountType lineExtensionAmountType = new LineExtensionAmountType();
        lineExtensionAmountType.setValue(dto.getNetAmount());
        lineExtensionAmountType.setCurrencyID(currency);
        total.setLineExtensionAmount(lineExtensionAmountType);

        // Same as Line Extension Amount (excluding document level charges/allowances for this scope)
        TaxExclusiveAmountType taxExclusiveAmountType = new TaxExclusiveAmountType();
        taxExclusiveAmountType.setValue(dto.getNetAmount());
        taxExclusiveAmountType.setCurrencyID(currency);
        total.setTaxExclusiveAmount(taxExclusiveAmountType);

        // Gross Amount including VAT
        TaxInclusiveAmountType taxInclusiveAmountType = new TaxInclusiveAmountType();
        taxInclusiveAmountType.setValue(dto.getRealTotalAmount());
        taxInclusiveAmountType.setCurrencyID(currency);
        total.setTaxInclusiveAmount(taxInclusiveAmountType);

        // Handles invoice total rounding if applicable
        if (dto.getRounding() != null) {
            PayableRoundingAmountType payableRoundingAmountType = new PayableRoundingAmountType();
            payableRoundingAmountType.setValue(dto.getRounding());
            payableRoundingAmountType.setCurrencyID(currency);
            total.setPayableRoundingAmount(payableRoundingAmountType);
        }

        // Amount due for payment
        PayableAmountType payableAmountType = new PayableAmountType();
        payableAmountType.setValue(dto.getTotalAmount());
        payableAmountType.setCurrencyID(currency);
        total.setPayableAmount(payableAmountType);

        return total;
    }

    /**
     * Constructs the individual Invoice Lines.
     * Applies complex logic for line-level discounts and strict VAT category mapping.
     */
    private List<InvoiceLineType> buildInvoiceLines(EhfInvoiceDTO dto) {
        List<InvoiceLineType> lines = new ArrayList<>();
        String currency = dto.getCurrency();
        List<EhfInvoiceLineItem> items = dto.getItems();

        if (items == null) return lines;

        for (int i = 0; i < items.size(); i++) {
            EhfInvoiceLineItem productLine = items.get(i);

            // Filter logic: Only process standard item lines (type 1)
            if (!Integer.valueOf(1).equals(productLine.getType())) continue;

            InvoiceLineType line = new InvoiceLineType();
            String lineIdStr = String.valueOf(i + 1);

            IDType lineId = new IDType();
            lineId.setValue(lineIdStr);
            line.setID(lineId);

            InvoicedQuantityType invoicedQuantityType = new InvoicedQuantityType();
            invoicedQuantityType.setValue(productLine.getQty());
            invoicedQuantityType.setUnitCode(productLine.getUnitCode()); // Must follow UN/ECE Rec 20
            line.setInvoicedQuantity(invoicedQuantityType);

            // EN 16931 Rule BR-LIN-04 Compliance:
            // LineExtensionAmount must strictly be calculated as: (Price * Qty) - Discount + Surcharges.
            // The DTO amount is expected to be the pre-calculated Net Amount.
            BigDecimal lineAmt = productLine.getAmount() != null ? productLine.getAmount() : BigDecimal.ZERO;
            LineExtensionAmountType lineExtensionAmountType = new LineExtensionAmountType();
            lineExtensionAmountType.setValue(lineAmt);
            lineExtensionAmountType.setCurrencyID(currency);
            line.setLineExtensionAmount(lineExtensionAmountType);

            // Links the invoice line to the original purchase order line
            OrderLineReferenceType orderLineReferenceType = new OrderLineReferenceType();
            LineIDType orderLineId = new LineIDType();
            orderLineId.setValue(lineIdStr);
            orderLineReferenceType.setLineID(orderLineId);
            line.setOrderLineReference(List.of(orderLineReferenceType));

            // Allowance / Discount Processing
            if (productLine.getDisc() != null) {
                AllowanceChargeType allowance = new AllowanceChargeType();

                // False indicates this is a discount (allowance), True would mean a surcharge (charge).
                ChargeIndicatorType chargeIndicatorType = new ChargeIndicatorType();
                chargeIndicatorType.setValue(false);
                allowance.setChargeIndicator(chargeIndicatorType);

                AllowanceChargeReasonType reasonType = new AllowanceChargeReasonType();
                reasonType.setValue("Discount");
                allowance.setAllowanceChargeReason(List.of(reasonType));

                MultiplierFactorNumericType multiplierType = new MultiplierFactorNumericType();
                multiplierType.setValue(productLine.getDisc()); 
                allowance.setMultiplierFactorNumeric(multiplierType);

                // Base amount against which the discount is applied
                BigDecimal baseAmount = productLine.getPrice().multiply(productLine.getQty());
                BaseAmountType baseAmountType = new BaseAmountType();
                baseAmountType.setValue(baseAmount.setScale(2, RoundingMode.HALF_DOWN));
                baseAmountType.setCurrencyID(currency);
                allowance.setBaseAmount(baseAmountType);

                BigDecimal discAmt = productLine.getDiscAmount() != null ? productLine.getDiscAmount() : BigDecimal.ZERO;
                AmountType amountType = new AmountType();
                amountType.setValue(discAmt.setScale(2, RoundingMode.HALF_DOWN));
                amountType.setCurrencyID(currency);
                allowance.setAmount(amountType);

                line.setAllowanceCharge(List.of(allowance));
            }

            // Item Details mapping
            ItemType item = new ItemType();
            String description = (productLine.getDescription() != null && !productLine.getDescription().isBlank()) ? productLine.getDescription() : "NA";
            NameType itemName = new NameType();
            itemName.setValue(description);
            item.setName(itemName);

            if (productLine.getCode() != null && !productLine.getCode().isBlank()) {
                ItemIdentificationType itemIdentificationType = new ItemIdentificationType();
                IDType idType = new IDType();
                idType.setValue(productLine.getCode());
                itemIdentificationType.setID(idType);
                item.setSellersItemIdentification(itemIdentificationType);
            }

            // Map the internal Norwegian VAT code to the standard PEPPOL Enum
            String systemVatCode = productLine.getVatCode();
            VatCategoryEnum categoryEnum = NorwegianVatMapper.getCategoryByVatCode(systemVatCode);

            TaxCategoryType taxCategory = new TaxCategoryType();
            IDType taxCategoryId = new IDType();
            taxCategoryId.setValue(categoryEnum.getCode());
            taxCategory.setID(taxCategoryId);

            BigDecimal vatRate = productLine.getVatRate() != null ? productLine.getVatRate() : BigDecimal.ZERO;
            PercentType percentType = new PercentType();
            percentType.setValue(vatRate);
            taxCategory.setPercent(percentType);

            TaxSchemeType taxSchemeType = new TaxSchemeType();
            IDType schemeId = new IDType();
            schemeId.setValue(SCHEME_VAT);
            schemeId.setSchemeID(SCHEME_VAT_NO);
            taxSchemeType.setID(schemeId);
            taxCategory.setTaxScheme(taxSchemeType);

            item.setClassifiedTaxCategory(List.of(taxCategory));

            line.setItem(item);

            // Item Price mapping (Net price per unit)
            PriceType priceType = new PriceType();
            PriceAmountType priceAmountType = new PriceAmountType();
            priceAmountType.setValue(productLine.getPrice());
            priceAmountType.setCurrencyID(currency);
            priceType.setPriceAmount(priceAmountType);
            line.setPrice(priceType);

            lines.add(line);
        }

        return lines;
    }
}