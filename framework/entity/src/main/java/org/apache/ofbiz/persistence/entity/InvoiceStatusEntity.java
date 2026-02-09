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
@Entity(name = "INVOICE_STATUS")
@Table(name = "INVOICE_STATUS")
public class InvoiceStatusEntity {
    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "STATUS_DATE")
    private Timestamp statusDate;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;
}
