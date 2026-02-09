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
@Entity(name = "PARTY_RESUME")
@Table(name = "PARTY_RESUME")
public class PartyResumeEntity {
    @Id
    @Column(name = "RESUME_ID")
    private String resumeId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "RESUME_DATE")
    private Timestamp resumeDate;

    @Column(name = "RESUME_TEXT")
    private String resumeText;
}
