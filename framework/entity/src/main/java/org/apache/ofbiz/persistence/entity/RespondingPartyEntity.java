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
@Entity(name = "RESPONDING_PARTY")
@Table(name = "RESPONDING_PARTY")
public class RespondingPartyEntity {
    @Id
    @Column(name = "RESPONDING_PARTY_SEQ_ID")
    private String respondingPartySeqId;

    @Id
    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "DATE_SENT")
    private Timestamp dateSent;
}
