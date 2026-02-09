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
@Entity(name = "CONTENT_PURPOSE_OPERATION")
@Table(name = "CONTENT_PURPOSE_OPERATION")
public class ContentPurposeOperationEntity {
    @Id
    @Column(name = "CONTENT_PURPOSE_TYPE_ID")
    private String contentPurposeTypeId;

    @Id
    @Column(name = "CONTENT_OPERATION_ID")
    private String contentOperationId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "PRIVILEGE_ENUM_ID")
    private String privilegeEnumId;
}
