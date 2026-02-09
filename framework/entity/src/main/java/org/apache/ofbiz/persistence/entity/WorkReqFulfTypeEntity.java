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
@Entity(name = "WORK_REQ_FULF_TYPE")
@Table(name = "WORK_REQ_FULF_TYPE")
public class WorkReqFulfTypeEntity {
    @Id
    @Column(name = "WORK_REQ_FULF_TYPE_ID")
    private String workReqFulfTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
