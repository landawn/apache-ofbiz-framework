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
@Entity(name = "STATUS_ITEM")
@Table(name = "STATUS_ITEM")
public class StatusItemEntity {
    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "STATUS_TYPE_ID")
    private String statusTypeId;

    @Column(name = "STATUS_CODE")
    private String statusCode;

    @Column(name = "SEQUENCE_ID")
    private String sequenceId;

    @Column(name = "DESCRIPTION")
    private String description;
}
