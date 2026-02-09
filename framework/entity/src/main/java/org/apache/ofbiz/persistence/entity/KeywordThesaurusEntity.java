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
@Entity(name = "KEYWORD_THESAURUS")
@Table(name = "KEYWORD_THESAURUS")
public class KeywordThesaurusEntity {
    @Id
    @Column(name = "ENTERED_KEYWORD")
    private String enteredKeyword;

    @Id
    @Column(name = "ALTERNATE_KEYWORD")
    private String alternateKeyword;

    @Column(name = "RELATIONSHIP_ENUM_ID")
    private String relationshipEnumId;
}
