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
@Entity(name = "USER_LOGIN_HISTORY")
@Table(name = "USER_LOGIN_HISTORY")
public class UserLoginHistoryEntity {
    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PASSWORD_USED")
    private String passwordUsed;

    @Column(name = "SUCCESSFUL_LOGIN")
    private String successfulLogin;

    @Column(name = "ORIGIN_USER_LOGIN_ID")
    private String originUserLoginId;

    @Column(name = "PARTY_ID")
    private String partyId;
}
