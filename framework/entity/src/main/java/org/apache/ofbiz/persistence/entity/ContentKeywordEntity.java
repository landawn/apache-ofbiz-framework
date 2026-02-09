package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CONTENT_KEYWORD")
@Table(name = "CONTENT_KEYWORD")
public class ContentKeywordEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "KEYWORD")
    private String keyword;

    @Column(name = "RELEVANCY_WEIGHT")
    private BigDecimal relevancyWeight;
}
