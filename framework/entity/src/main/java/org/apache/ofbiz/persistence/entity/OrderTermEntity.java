package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDER_TERM")
@Table(name = "ORDER_TERM")
public class OrderTermEntity {
    @Id
    @Column(name = "TERM_TYPE_ID")
    private String termTypeId;

    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "TERM_VALUE")
    private BigDecimal termValue;

    @Column(name = "TERM_DAYS")
    private BigDecimal termDays;

    @Column(name = "TEXT_VALUE")
    private String textValue;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "UOM_ID")
    private String uomId;
}
