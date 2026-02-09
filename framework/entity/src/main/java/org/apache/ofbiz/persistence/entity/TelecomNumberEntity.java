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
@Entity(name = "TELECOM_NUMBER")
@Table(name = "TELECOM_NUMBER")
public class TelecomNumberEntity {
    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "AREA_CODE")
    private String areaCode;

    @Column(name = "CONTACT_NUMBER")
    private String contactNumber;

    @Column(name = "ASK_FOR_NAME")
    private String askForName;
}
