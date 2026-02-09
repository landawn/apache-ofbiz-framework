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
@Entity(name = "CONTACT_MECH")
@Table(name = "CONTACT_MECH")
public class ContactMechEntity {
    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "CONTACT_MECH_TYPE_ID")
    private String contactMechTypeId;

    @Column(name = "INFO_STRING")
    private String infoString;
}
