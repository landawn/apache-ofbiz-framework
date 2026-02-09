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
@Entity(name = "WEB_USER_PREFERENCE")
@Table(name = "WEB_USER_PREFERENCE")
public class WebUserPreferenceEntity {
    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "WEB_PREFERENCE_TYPE_ID")
    private String webPreferenceTypeId;

    @Column(name = "WEB_PREFERENCE_VALUE")
    private String webPreferenceValue;
}
