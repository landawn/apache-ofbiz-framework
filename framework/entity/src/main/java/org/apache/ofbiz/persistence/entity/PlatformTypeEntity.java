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
@Entity(name = "PLATFORM_TYPE")
@Table(name = "PLATFORM_TYPE")
public class PlatformTypeEntity {
    @Id
    @Column(name = "PLATFORM_TYPE_ID")
    private String platformTypeId;

    @Column(name = "PLATFORM_NAME")
    private String platformName;

    @Column(name = "PLATFORM_VERSION")
    private String platformVersion;
}
