package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT_PAYMENT_METHOD_TYPE")
@Table(name = "PRODUCT_PAYMENT_METHOD_TYPE")
public class ProductPaymentMethodTypeEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Id
    @Column(name = "PRODUCT_PRICE_PURPOSE_ID")
    private String productPricePurposeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
