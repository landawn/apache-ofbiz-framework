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
@Entity(name = "FTP_ADDRESS")
@Table(name = "FTP_ADDRESS")
public class FtpAddressEntity {
    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "HOSTNAME")
    private String hostname;

    @Column(name = "PORT")
    private BigDecimal port;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "FTP_PASSWORD")
    private String ftpPassword;

    @Column(name = "BINARY_TRANSFER")
    private String binaryTransfer;

    @Column(name = "FILE_PATH")
    private String filePath;

    @Column(name = "ZIP_FILE")
    private String zipFile;

    @Column(name = "PASSIVE_MODE")
    private String passiveMode;

    @Column(name = "DEFAULT_TIMEOUT")
    private BigDecimal defaultTimeout;
}
