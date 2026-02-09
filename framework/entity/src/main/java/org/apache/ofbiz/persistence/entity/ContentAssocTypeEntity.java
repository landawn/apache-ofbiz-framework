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
@Entity(name = "CONTENT_ASSOC_TYPE")
@Table(name = "CONTENT_ASSOC_TYPE")
public class ContentAssocTypeEntity {
    @Id
    @Column(name = "CONTENT_ASSOC_TYPE_ID")
    private String contentAssocTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
