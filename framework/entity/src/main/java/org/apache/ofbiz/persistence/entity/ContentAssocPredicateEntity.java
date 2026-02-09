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
@Entity(name = "CONTENT_ASSOC_PREDICATE")
@Table(name = "CONTENT_ASSOC_PREDICATE")
public class ContentAssocPredicateEntity {
    @Id
    @Column(name = "CONTENT_ASSOC_PREDICATE_ID")
    private String contentAssocPredicateId;

    @Column(name = "DESCRIPTION")
    private String description;
}
