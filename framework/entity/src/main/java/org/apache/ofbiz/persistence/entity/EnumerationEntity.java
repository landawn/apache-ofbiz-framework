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
@Entity(name = "ENUMERATION")
@Table(name = "ENUMERATION")
public class EnumerationEntity {
    @Id
    @Column(name = "ENUM_ID")
    private String enumId;

    @Column(name = "ENUM_TYPE_ID")
    private String enumTypeId;

    @Column(name = "ENUM_CODE")
    private String enumCode;

    @Column(name = "SEQUENCE_ID")
    private String sequenceId;

    @Column(name = "DESCRIPTION")
    private String description;
}
