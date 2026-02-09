package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RETURN_HEADER")
@Table(name = "RETURN_HEADER")
public class ReturnHeaderEntity {
    @Id
    @Column(name = "RETURN_ID")
    private String returnId;

    @Column(name = "RETURN_HEADER_TYPE_ID")
    private String returnHeaderTypeId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "FROM_PARTY_ID")
    private String fromPartyId;

    @Column(name = "TO_PARTY_ID")
    private String toPartyId;

    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "ENTRY_DATE")
    private Timestamp entryDate;

    @Column(name = "ORIGIN_CONTACT_MECH_ID")
    private String originContactMechId;

    @Column(name = "DESTINATION_FACILITY_ID")
    private String destinationFacilityId;

    @Column(name = "NEEDS_INVENTORY_RECEIVE")
    private String needsInventoryReceive;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "SUPPLIER_RMA_ID")
    private String supplierRmaId;
}
