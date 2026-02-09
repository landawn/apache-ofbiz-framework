package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SEQUENCE_VALUE_ITEM")
@Table(name = "SEQUENCE_VALUE_ITEM")
public class SequenceValueItemEntity {
    @Id
    @Column(name = "SEQ_NAME")
    private String seqName;

    @Column(name = "SEQ_ID")
    private BigDecimal seqId;
}
