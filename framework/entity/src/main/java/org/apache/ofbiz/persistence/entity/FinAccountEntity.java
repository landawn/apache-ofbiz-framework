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
@Entity(name = "FIN_ACCOUNT")
@Table(name = "FIN_ACCOUNT")
public class FinAccountEntity {
    @Id
    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Column(name = "FIN_ACCOUNT_TYPE_ID")
    private String finAccountTypeId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "FIN_ACCOUNT_NAME")
    private String finAccountName;

    @Column(name = "FIN_ACCOUNT_CODE")
    private String finAccountCode;

    @Column(name = "FIN_ACCOUNT_PIN")
    private String finAccountPin;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "OWNER_PARTY_ID")
    private String ownerPartyId;

    @Column(name = "POST_TO_GL_ACCOUNT_ID")
    private String postToGlAccountId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "IS_REFUNDABLE")
    private String isRefundable;

    @Column(name = "REPLENISH_PAYMENT_ID")
    private String replenishPaymentId;

    @Column(name = "REPLENISH_LEVEL")
    private BigDecimal replenishLevel;

    @Column(name = "ACTUAL_BALANCE")
    private BigDecimal actualBalance;

    @Column(name = "AVAILABLE_BALANCE")
    private BigDecimal availableBalance;
}
