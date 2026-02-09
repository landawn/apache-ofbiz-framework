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
@Entity(name = "PARTY_QUAL")
@Table(name = "PARTY_QUAL")
public class PartyQualEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "PARTY_QUAL_TYPE_ID")
    private String partyQualTypeId;

    @Column(name = "QUALIFICATION_DESC")
    private String qualificationDesc;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "VERIF_STATUS_ID")
    private String verifStatusId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
