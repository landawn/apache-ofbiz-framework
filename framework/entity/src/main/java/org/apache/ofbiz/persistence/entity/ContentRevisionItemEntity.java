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
@Entity(name = "CONTENT_REVISION_ITEM")
@Table(name = "CONTENT_REVISION_ITEM")
public class ContentRevisionItemEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "CONTENT_REVISION_SEQ_ID")
    private String contentRevisionSeqId;

    @Id
    @Column(name = "ITEM_CONTENT_ID")
    private String itemContentId;

    @Column(name = "OLD_DATA_RESOURCE_ID")
    private String oldDataResourceId;

    @Column(name = "NEW_DATA_RESOURCE_ID")
    private String newDataResourceId;
}
