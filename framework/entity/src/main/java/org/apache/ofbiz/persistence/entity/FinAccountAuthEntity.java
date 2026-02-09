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
@Entity(name = "FIN_ACCOUNT_AUTH")
@Table(name = "FIN_ACCOUNT_AUTH")
public class FinAccountAuthEntity {
    @Id
    @Column(name = "FIN_ACCOUNT_AUTH_ID")
    private String finAccountAuthId;

    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "AUTHORIZATION_DATE")
    private Timestamp authorizationDate;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
