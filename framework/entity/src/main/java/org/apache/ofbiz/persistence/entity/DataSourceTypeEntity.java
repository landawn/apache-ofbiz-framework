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
@Entity(name = "DATA_SOURCE_TYPE")
@Table(name = "DATA_SOURCE_TYPE")
public class DataSourceTypeEntity {
    @Id
    @Column(name = "DATA_SOURCE_TYPE_ID")
    private String dataSourceTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
