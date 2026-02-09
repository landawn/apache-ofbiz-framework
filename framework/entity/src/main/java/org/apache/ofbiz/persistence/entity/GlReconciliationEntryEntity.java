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
@Entity(name = "GL_RECONCILIATION_ENTRY")
@Table(name = "GL_RECONCILIATION_ENTRY")
public class GlReconciliationEntryEntity {
    @Id
    @Column(name = "GL_RECONCILIATION_ID")
    private String glReconciliationId;

    @Id
    @Column(name = "ACCTG_TRANS_ID")
    private String acctgTransId;

    @Id
    @Column(name = "ACCTG_TRANS_ENTRY_SEQ_ID")
    private String acctgTransEntrySeqId;

    @Column(name = "RECONCILED_AMOUNT")
    private BigDecimal reconciledAmount;
}
