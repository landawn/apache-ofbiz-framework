package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TESTING_CRYPTO")
@Table(name = "TESTING_CRYPTO")
public class TestingCryptoEntity {
    @Id
    @Column(name = "TESTING_CRYPTO_ID")
    private String testingCryptoId;

    @Column(name = "TESTING_CRYPTO_TYPE_ID")
    private String testingCryptoTypeId;

    @Column(name = "UNENCRYPTED_VALUE")
    private String unencryptedValue;

    @Column(name = "ENCRYPTED_VALUE")
    private String encryptedValue;

    @Column(name = "SALTED_ENCRYPTED_VALUE")
    private String saltedEncryptedValue;
}
