package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "FACILITY_CONTACT_MECH_PURPOSE")
@Table(name = "FACILITY_CONTACT_MECH_PURPOSE")
public class FacilityContactMechPurposeEntity {
    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Id
    @Column(name = "CONTACT_MECH_PURPOSE_TYPE_ID")
    private String contactMechPurposeTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
