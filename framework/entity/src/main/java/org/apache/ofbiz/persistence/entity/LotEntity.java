package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "LOT")
@Table(name = "LOT")
public class LotEntity {
    @Id
    @Column(name = "LOT_ID")
    private String lotId;

    @Column(name = "CREATION_DATE")
    private Timestamp creationDate;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "EXPIRATION_DATE")
    private Timestamp expirationDate;
}
