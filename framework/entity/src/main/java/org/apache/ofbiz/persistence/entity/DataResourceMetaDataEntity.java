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
@Entity(name = "DATA_RESOURCE_META_DATA")
@Table(name = "DATA_RESOURCE_META_DATA")
public class DataResourceMetaDataEntity {
    @Id
    @Column(name = "DATA_RESOURCE_ID")
    private String dataResourceId;

    @Id
    @Column(name = "META_DATA_PREDICATE_ID")
    private String metaDataPredicateId;

    @Column(name = "META_DATA_VALUE")
    private String metaDataValue;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;
}
