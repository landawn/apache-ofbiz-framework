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
@Entity(name = "REJECTION_REASON")
@Table(name = "REJECTION_REASON")
public class RejectionReasonEntity {
    @Id
    @Column(name = "REJECTION_ID")
    private String rejectionId;

    @Column(name = "DESCRIPTION")
    private String description;
}
