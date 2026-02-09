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
@Entity(name = "MARKETING_CAMPAIGN_NOTE")
@Table(name = "MARKETING_CAMPAIGN_NOTE")
public class MarketingCampaignNoteEntity {
    @Id
    @Column(name = "MARKETING_CAMPAIGN_ID")
    private String marketingCampaignId;

    @Id
    @Column(name = "NOTE_ID")
    private String noteId;
}
