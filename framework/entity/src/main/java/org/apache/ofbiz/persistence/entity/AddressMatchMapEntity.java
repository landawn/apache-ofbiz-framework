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
@Entity(name = "ADDRESS_MATCH_MAP")
@Table(name = "ADDRESS_MATCH_MAP")
public class AddressMatchMapEntity {
    @Id
    @Column(name = "MAP_KEY")
    private String mapKey;

    @Id
    @Column(name = "MAP_VALUE")
    private String mapValue;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
