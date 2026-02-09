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
@Entity(name = "DATA_CATEGORY")
@Table(name = "DATA_CATEGORY")
public class DataCategoryEntity {
    @Id
    @Column(name = "DATA_CATEGORY_ID")
    private String dataCategoryId;

    @Column(name = "PARENT_CATEGORY_ID")
    private String parentCategoryId;

    @Column(name = "CATEGORY_NAME")
    private String categoryName;
}
