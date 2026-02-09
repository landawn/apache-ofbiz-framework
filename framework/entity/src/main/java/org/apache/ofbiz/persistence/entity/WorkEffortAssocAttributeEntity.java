package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "WORK_EFFORT_ASSOC_ATTRIBUTE")
@Table(name = "WORK_EFFORT_ASSOC_ATTRIBUTE")
public class WorkEffortAssocAttributeEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID_FROM")
    private String workEffortIdFrom;

    @Id
    @Column(name = "WORK_EFFORT_ID_TO")
    private String workEffortIdTo;

    @Id
    @Column(name = "WORK_EFFORT_ASSOC_TYPE_ID")
    private String workEffortAssocTypeId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;

    @Column(name = "ATTR_DESCRIPTION")
    private String attrDescription;
}
