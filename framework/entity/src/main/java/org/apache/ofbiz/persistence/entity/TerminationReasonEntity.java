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
@Entity(name = "TERMINATION_REASON")
@Table(name = "TERMINATION_REASON")
public class TerminationReasonEntity {
    @Id
    @Column(name = "TERMINATION_REASON_ID")
    private String terminationReasonId;

    @Column(name = "DESCRIPTION")
    private String description;
}
