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
@Entity(name = "SERVER_HIT")
@Table(name = "SERVER_HIT")
public class ServerHitEntity {
    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "HIT_START_DATE_TIME")
    private Timestamp hitStartDateTime;

    @Id
    @Column(name = "HIT_TYPE_ID")
    private String hitTypeId;

    @Column(name = "NUM_OF_BYTES")
    private BigDecimal numOfBytes;

    @Column(name = "RUNNING_TIME_MILLIS")
    private BigDecimal runningTimeMillis;

    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "REQUEST_URL")
    private String requestUrl;

    @Column(name = "REFERRER_URL")
    private String referrerUrl;

    @Column(name = "SERVER_IP_ADDRESS")
    private String serverIpAddress;

    @Column(name = "SERVER_HOST_NAME")
    private String serverHostName;

    @Column(name = "INTERNAL_CONTENT_ID")
    private String internalContentId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ID_BY_IP_CONTACT_MECH_ID")
    private String idByIpContactMechId;

    @Column(name = "REF_BY_WEB_CONTACT_MECH_ID")
    private String refByWebContactMechId;
}
