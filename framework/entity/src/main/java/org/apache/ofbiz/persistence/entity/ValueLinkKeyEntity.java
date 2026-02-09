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
@Entity(name = "VALUE_LINK_KEY")
@Table(name = "VALUE_LINK_KEY")
public class ValueLinkKeyEntity {
    @Id
    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "PUBLIC_KEY")
    private String publicKey;

    @Column(name = "PRIVATE_KEY")
    private String privateKey;

    @Column(name = "EXCHANGE_KEY")
    private String exchangeKey;

    @Column(name = "WORKING_KEY")
    private String workingKey;

    @Column(name = "WORKING_KEY_INDEX")
    private BigDecimal workingKeyIndex;

    @Column(name = "LAST_WORKING_KEY")
    private String lastWorkingKey;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_TERMINAL")
    private String createdByTerminal;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_TERMINAL")
    private String lastModifiedByTerminal;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
