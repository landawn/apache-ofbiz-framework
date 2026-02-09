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
@Entity(name = "USER_PREFERENCE")
@Table(name = "USER_PREFERENCE")
public class UserPreferenceEntity {
    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Id
    @Column(name = "USER_PREF_TYPE_ID")
    private String userPrefTypeId;

    @Column(name = "USER_PREF_GROUP_TYPE_ID")
    private String userPrefGroupTypeId;

    @Column(name = "USER_PREF_VALUE")
    private String userPrefValue;

    @Column(name = "USER_PREF_DATA_TYPE")
    private String userPrefDataType;
}
