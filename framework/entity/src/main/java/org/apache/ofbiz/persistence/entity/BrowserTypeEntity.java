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
@Entity(name = "BROWSER_TYPE")
@Table(name = "BROWSER_TYPE")
public class BrowserTypeEntity {
    @Id
    @Column(name = "BROWSER_TYPE_ID")
    private String browserTypeId;

    @Column(name = "BROWSER_NAME")
    private String browserName;

    @Column(name = "BROWSER_VERSION")
    private String browserVersion;
}
