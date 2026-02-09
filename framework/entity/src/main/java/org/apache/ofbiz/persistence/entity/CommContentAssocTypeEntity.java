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
@Entity(name = "COMM_CONTENT_ASSOC_TYPE")
@Table(name = "COMM_CONTENT_ASSOC_TYPE")
public class CommContentAssocTypeEntity {
    @Id
    @Column(name = "COMM_CONTENT_ASSOC_TYPE_ID")
    private String commContentAssocTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
