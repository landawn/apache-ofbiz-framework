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
@Entity(name = "CONTENT")
@Table(name = "CONTENT")
public class ContentEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "CONTENT_TYPE_ID")
    private String contentTypeId;

    @Column(name = "OWNER_CONTENT_ID")
    private String ownerContentId;

    @Column(name = "DECORATOR_CONTENT_ID")
    private String decoratorContentId;

    @Column(name = "INSTANCE_OF_CONTENT_ID")
    private String instanceOfContentId;

    @Column(name = "DATA_RESOURCE_ID")
    private String dataResourceId;

    @Column(name = "TEMPLATE_DATA_RESOURCE_ID")
    private String templateDataResourceId;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PRIVILEGE_ENUM_ID")
    private String privilegeEnumId;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "CUSTOM_METHOD_ID")
    private String customMethodId;

    @Column(name = "CONTENT_NAME")
    private String contentName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LOCALE_STRING")
    private String localeString;

    @Column(name = "MIME_TYPE_ID")
    private String mimeTypeId;

    @Column(name = "CHARACTER_SET_ID")
    private String characterSetId;

    @Column(name = "CHILD_LEAF_COUNT")
    private BigDecimal childLeafCount;

    @Column(name = "CHILD_BRANCH_COUNT")
    private BigDecimal childBranchCount;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
