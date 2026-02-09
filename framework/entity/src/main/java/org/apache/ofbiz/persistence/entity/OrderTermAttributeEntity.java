package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDER_TERM_ATTRIBUTE")
@Table(name = "ORDER_TERM_ATTRIBUTE")
public class OrderTermAttributeEntity {
    @Id
    @Column(name = "TERM_TYPE_ID")
    private String termTypeId;

    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;

    @Column(name = "ATTR_DESCRIPTION")
    private String attrDescription;
}
