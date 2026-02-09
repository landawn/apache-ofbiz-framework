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
@Entity(name = "REQUIREMENT_CUST_REQUEST")
@Table(name = "REQUIREMENT_CUST_REQUEST")
public class RequirementCustRequestEntity {
    @Id
    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Id
    @Column(name = "CUST_REQUEST_ITEM_SEQ_ID")
    private String custRequestItemSeqId;

    @Id
    @Column(name = "REQUIREMENT_ID")
    private String requirementId;
}
