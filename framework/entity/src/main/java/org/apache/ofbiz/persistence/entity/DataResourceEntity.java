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
@Entity(name = "DATA_RESOURCE")
@Table(name = "DATA_RESOURCE")
public class DataResourceEntity {
    @Id
    @Column(name = "DATA_RESOURCE_ID")
    private String dataResourceId;

    @Column(name = "DATA_RESOURCE_TYPE_ID")
    private String dataResourceTypeId;

    @Column(name = "DATA_TEMPLATE_TYPE_ID")
    private String dataTemplateTypeId;

    @Column(name = "DATA_CATEGORY_ID")
    private String dataCategoryId;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "DATA_RESOURCE_NAME")
    private String dataResourceName;

    @Column(name = "LOCALE_STRING")
    private String localeString;

    @Column(name = "MIME_TYPE_ID")
    private String mimeTypeId;

    @Column(name = "CHARACTER_SET_ID")
    private String characterSetId;

    @Column(name = "OBJECT_INFO")
    private String objectInfo;

    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Column(name = "SURVEY_RESPONSE_ID")
    private String surveyResponseId;

    @Column(name = "RELATED_DETAIL_ID")
    private String relatedDetailId;

    @Column(name = "IS_PUBLIC")
    private String isPublic;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
