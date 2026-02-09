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
@Entity(name = "CONTENT_OPERATION")
@Table(name = "CONTENT_OPERATION")
public class ContentOperationEntity {
    @Id
    @Column(name = "CONTENT_OPERATION_ID")
    private String contentOperationId;

    @Column(name = "DESCRIPTION")
    private String description;
}
