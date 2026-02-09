package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "USER_LOGIN")
@Table(name = "USER_LOGIN")
public class UserLoginEntity {
    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "CURRENT_PASSWORD")
    private String currentPassword;

    @Column(name = "PASSWORD_HINT")
    private String passwordHint;

    @Column(name = "IS_SYSTEM")
    private String isSystem;

    @Column(name = "ENABLED")
    private String enabled;

    @Column(name = "HAS_LOGGED_OUT")
    private String hasLoggedOut;

    @Column(name = "REQUIRE_PASSWORD_CHANGE")
    private String requirePasswordChange;

    @Column(name = "LAST_CURRENCY_UOM")
    private String lastCurrencyUom;

    @Column(name = "LAST_LOCALE")
    private String lastLocale;

    @Column(name = "LAST_TIME_ZONE")
    private String lastTimeZone;

    @Column(name = "DISABLED_DATE_TIME")
    private Timestamp disabledDateTime;

    @Column(name = "SUCCESSIVE_FAILED_LOGINS")
    private BigDecimal successiveFailedLogins;

    @Column(name = "EXTERNAL_AUTH_ID")
    private String externalAuthId;

    @Column(name = "USER_LDAP_DN")
    private String userLdapDn;

    @Column(name = "DISABLED_BY")
    private String disabledBy;

    @Column(name = "PARTY_ID")
    private String partyId;
}
