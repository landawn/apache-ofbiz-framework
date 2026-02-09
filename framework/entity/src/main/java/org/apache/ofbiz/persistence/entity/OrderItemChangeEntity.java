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
@Entity(name = "ORDER_ITEM_CHANGE")
@Table(name = "ORDER_ITEM_CHANGE")
public class OrderItemChangeEntity {
    @Id
    @Column(name = "ORDER_ITEM_CHANGE_ID")
    private String orderItemChangeId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "CHANGE_TYPE_ENUM_ID")
    private String changeTypeEnumId;

    @Column(name = "CHANGE_DATETIME")
    private Timestamp changeDatetime;

    @Column(name = "CHANGE_USER_LOGIN")
    private String changeUserLogin;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "CANCEL_QUANTITY")
    private BigDecimal cancelQuantity;

    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "ITEM_DESCRIPTION")
    private String itemDescription;

    @Column(name = "REASON_ENUM_ID")
    private String reasonEnumId;

    @Column(name = "CHANGE_COMMENTS")
    private String changeComments;
}
