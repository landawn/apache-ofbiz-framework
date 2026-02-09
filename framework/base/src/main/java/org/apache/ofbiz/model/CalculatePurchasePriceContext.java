package org.apache.ofbiz.model;

import java.math.BigDecimal;
import java.util.Locale;

import org.apache.ofbiz.persistence.entity.ProductEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CalculatePurchasePriceContext {
    private ProductEntity product;
    private String agreementId;
    private String currencyUomId;
    private String partyId;
    private BigDecimal quantity;
    private Locale locale;
}
