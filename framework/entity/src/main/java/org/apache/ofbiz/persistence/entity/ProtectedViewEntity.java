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
@Entity(name = "PROTECTED_VIEW")
@Table(name = "PROTECTED_VIEW")
public class ProtectedViewEntity {
    @Id
    @Column(name = "GROUP_ID")
    private String groupId;

    @Id
    @Column(name = "VIEW_NAME_ID")
    private String viewNameId;

    @Column(name = "MAX_HITS")
    private BigDecimal maxHits;

    @Column(name = "MAX_HITS_DURATION")
    private BigDecimal maxHitsDuration;

    @Column(name = "TARPIT_DURATION")
    private BigDecimal tarpitDuration;
}
