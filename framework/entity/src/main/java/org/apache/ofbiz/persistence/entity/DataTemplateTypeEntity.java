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
@Entity(name = "DATA_TEMPLATE_TYPE")
@Table(name = "DATA_TEMPLATE_TYPE")
public class DataTemplateTypeEntity {
    @Id
    @Column(name = "DATA_TEMPLATE_TYPE_ID")
    private String dataTemplateTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "EXTENSION")
    private String extension;
}
