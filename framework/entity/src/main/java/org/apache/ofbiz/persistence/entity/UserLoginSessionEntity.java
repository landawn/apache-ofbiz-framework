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
@Entity(name = "USER_LOGIN_SESSION")
@Table(name = "USER_LOGIN_SESSION")
public class UserLoginSessionEntity {
    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "SAVED_DATE")
    private Timestamp savedDate;

    @Column(name = "SESSION_DATA")
    private String sessionData;
}
