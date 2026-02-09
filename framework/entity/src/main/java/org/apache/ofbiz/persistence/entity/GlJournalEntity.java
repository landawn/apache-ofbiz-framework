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
@Entity(name = "GL_JOURNAL")
@Table(name = "GL_JOURNAL")
public class GlJournalEntity {
    @Id
    @Column(name = "GL_JOURNAL_ID")
    private String glJournalId;

    @Column(name = "GL_JOURNAL_NAME")
    private String glJournalName;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "IS_POSTED")
    private String isPosted;

    @Column(name = "POSTED_DATE")
    private Timestamp postedDate;
}
