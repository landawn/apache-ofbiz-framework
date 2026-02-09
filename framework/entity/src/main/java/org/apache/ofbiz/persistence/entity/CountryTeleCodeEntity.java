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
@Entity(name = "COUNTRY_TELE_CODE")
@Table(name = "COUNTRY_TELE_CODE")
public class CountryTeleCodeEntity {
    @Id
    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "TELE_CODE")
    private String teleCode;
}
