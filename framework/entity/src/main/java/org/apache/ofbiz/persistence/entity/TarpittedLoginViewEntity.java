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
@Entity(name = "TARPITTED_LOGIN_VIEW")
@Table(name = "TARPITTED_LOGIN_VIEW")
public class TarpittedLoginViewEntity {
    @Id
    @Column(name = "VIEW_NAME_ID")
    private String viewNameId;

    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "TARPIT_RELEASE_DATE_TIME")
    private BigDecimal tarpitReleaseDateTime;
}
