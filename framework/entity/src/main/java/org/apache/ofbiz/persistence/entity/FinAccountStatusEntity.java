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
@Entity(name = "FIN_ACCOUNT_STATUS")
@Table(name = "FIN_ACCOUNT_STATUS")
public class FinAccountStatusEntity {
    @Id
    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "STATUS_DATE")
    private Timestamp statusDate;

    @Column(name = "STATUS_END_DATE")
    private Timestamp statusEndDate;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;
}
