package org.apache.ofbiz.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CalculateProductPriceContext {
    private ProductEntity product;
    private String prodCatalogId;
    private String webSiteId;
    private String checkIncludeVat;
    private String surveyResponseId;
    private Map<String, Object> customAttributes;
    private String findAllQuantityPrices;
    private String optimizeForLargeRuleSet;
    private String agreementId;
    private String productStoreId;
    private String productStoreGroupId;
    private Locale locale;
    private String currencyUomId;
    private String currencyUomIdTo;
    private String productPricePurposeId;
    private String termUomId;
    private String partyId;
    private UserLoginEntity userLogin;
    private UserLoginEntity autoUserLogin;
    private BigDecimal quantity;
    private BigDecimal amount;
}
