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
@Entity(name = "VISIT")
@Table(name = "VISIT")
public class VisitEntity {
    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Column(name = "VISITOR_ID")
    private String visitorId;

    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "USER_CREATED")
    private String userCreated;

    @Column(name = "SESSION_ID")
    private String sessionId;

    @Column(name = "SERVER_IP_ADDRESS")
    private String serverIpAddress;

    @Column(name = "SERVER_HOST_NAME")
    private String serverHostName;

    @Column(name = "WEBAPP_NAME")
    private String webappName;

    @Column(name = "INITIAL_LOCALE")
    private String initialLocale;

    @Column(name = "INITIAL_REQUEST")
    private String initialRequest;

    @Column(name = "INITIAL_REFERRER")
    private String initialReferrer;

    @Column(name = "INITIAL_USER_AGENT")
    private String initialUserAgent;

    @Column(name = "USER_AGENT_ID")
    private String userAgentId;

    @Column(name = "CLIENT_IP_ADDRESS")
    private String clientIpAddress;

    @Column(name = "CLIENT_HOST_NAME")
    private String clientHostName;

    @Column(name = "CLIENT_USER")
    private String clientUser;

    @Column(name = "CLIENT_IP_ISP_NAME")
    private String clientIpIspName;

    @Column(name = "CLIENT_IP_POSTAL_CODE")
    private String clientIpPostalCode;

    @Column(name = "COOKIE")
    private String cookie;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "CLIENT_IP_STATE_PROV_GEO_ID")
    private String clientIpStateProvGeoId;

    @Column(name = "CLIENT_IP_COUNTRY_GEO_ID")
    private String clientIpCountryGeoId;
}
