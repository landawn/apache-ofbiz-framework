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
@Entity(name = "DATA_RESOURCE_PURPOSE")
@Table(name = "DATA_RESOURCE_PURPOSE")
public class DataResourcePurposeEntity {
    @Id
    @Column(name = "DATA_RESOURCE_ID")
    private String dataResourceId;

    @Id
    @Column(name = "CONTENT_PURPOSE_TYPE_ID")
    private String contentPurposeTypeId;
}
