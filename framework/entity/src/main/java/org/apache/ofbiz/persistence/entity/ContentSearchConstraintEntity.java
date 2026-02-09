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
@Entity(name = "CONTENT_SEARCH_CONSTRAINT")
@Table(name = "CONTENT_SEARCH_CONSTRAINT")
public class ContentSearchConstraintEntity {
    @Id
    @Column(name = "CONTENT_SEARCH_RESULT_ID")
    private String contentSearchResultId;

    @Id
    @Column(name = "CONSTRAINT_SEQ_ID")
    private String constraintSeqId;

    @Column(name = "CONSTRAINT_NAME")
    private String constraintName;

    @Column(name = "INFO_STRING")
    private String infoString;

    @Column(name = "INCLUDE_SUB_CATEGORIES")
    private String includeSubCategories;

    @Column(name = "IS_AND")
    private String isAnd;

    @Column(name = "ANY_PREFIX")
    private String anyPrefix;

    @Column(name = "ANY_SUFFIX")
    private String anySuffix;

    @Column(name = "REMOVE_STEMS")
    private String removeStems;

    @Column(name = "LOW_VALUE")
    private String lowValue;

    @Column(name = "HIGH_VALUE")
    private String highValue;
}
