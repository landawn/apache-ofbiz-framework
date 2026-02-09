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
@Entity(name = "RETURN_STATUS")
@Table(name = "RETURN_STATUS")
public class ReturnStatusEntity {
    @Id
    @Column(name = "RETURN_STATUS_ID")
    private String returnStatusId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "RETURN_ID")
    private String returnId;

    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;

    @Column(name = "STATUS_DATETIME")
    private Timestamp statusDatetime;
}
