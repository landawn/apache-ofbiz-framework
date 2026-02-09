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
@Entity(name = "RETURN_REASON")
@Table(name = "RETURN_REASON")
public class ReturnReasonEntity {
    @Id
    @Column(name = "RETURN_REASON_ID")
    private String returnReasonId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SEQUENCE_ID")
    private String sequenceId;
}
