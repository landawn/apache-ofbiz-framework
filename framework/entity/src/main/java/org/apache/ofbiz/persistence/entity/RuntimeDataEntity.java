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
@Entity(name = "RUNTIME_DATA")
@Table(name = "RUNTIME_DATA")
public class RuntimeDataEntity {
    @Id
    @Column(name = "RUNTIME_DATA_ID")
    private String runtimeDataId;

    @Column(name = "RUNTIME_INFO")
    private String runtimeInfo;
}
