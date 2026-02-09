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
@Entity(name = "EMAIL_ADDRESS_VERIFICATION")
@Table(name = "EMAIL_ADDRESS_VERIFICATION")
public class EmailAddressVerificationEntity {
    @Id
    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;

    @Column(name = "VERIFY_HASH")
    private String verifyHash;

    @Column(name = "EXPIRE_DATE")
    private Timestamp expireDate;
}
