package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CONTENT_ASSOC")
@Table(name = "CONTENT_ASSOC")
public class ContentAssocEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "CONTENT_ID_TO")
    private String contentIdTo;

    @Id
    @Column(name = "CONTENT_ASSOC_TYPE_ID")
    private String contentAssocTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "CONTENT_ASSOC_PREDICATE_ID")
    private String contentAssocPredicateId;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "MAP_KEY")
    private String mapKey;

    @Column(name = "UPPER_COORDINATE")
    private BigDecimal upperCoordinate;

    @Column(name = "LEFT_COORDINATE")
    private BigDecimal leftCoordinate;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
