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
@Entity(name = "TELECOM_METHOD_TYPE")
@Table(name = "TELECOM_METHOD_TYPE")
public class TelecomMethodTypeEntity {
    @Id
    @Column(name = "TELECOM_METHOD_TYPE_ID")
    private String telecomMethodTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
