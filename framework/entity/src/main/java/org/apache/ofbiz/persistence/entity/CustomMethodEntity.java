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
@Entity(name = "CUSTOM_METHOD")
@Table(name = "CUSTOM_METHOD")
public class CustomMethodEntity {
    @Id
    @Column(name = "CUSTOM_METHOD_ID")
    private String customMethodId;

    @Column(name = "CUSTOM_METHOD_TYPE_ID")
    private String customMethodTypeId;

    @Column(name = "CUSTOM_METHOD_NAME")
    private String customMethodName;

    @Column(name = "DESCRIPTION")
    private String description;
}
