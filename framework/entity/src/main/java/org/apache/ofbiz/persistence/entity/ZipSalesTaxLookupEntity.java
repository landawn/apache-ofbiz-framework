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
@Entity(name = "ZIP_SALES_TAX_LOOKUP")
@Table(name = "ZIP_SALES_TAX_LOOKUP")
public class ZipSalesTaxLookupEntity {
    @Id
    @Column(name = "ZIP_CODE")
    private String zipCode;

    @Id
    @Column(name = "STATE_CODE")
    private String stateCode;

    @Id
    @Column(name = "CITY")
    private String city;

    @Id
    @Column(name = "COUNTY")
    private String county;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "COUNTY_FIPS")
    private String countyFips;

    @Column(name = "COUNTY_DEFAULT")
    private String countyDefault;

    @Column(name = "GENERAL_DEFAULT")
    private String generalDefault;

    @Column(name = "INSIDE_CITY")
    private String insideCity;

    @Column(name = "GEO_CODE")
    private String geoCode;

    @Column(name = "STATE_SALES_TAX")
    private BigDecimal stateSalesTax;

    @Column(name = "CITY_SALES_TAX")
    private BigDecimal citySalesTax;

    @Column(name = "CITY_LOCAL_SALES_TAX")
    private BigDecimal cityLocalSalesTax;

    @Column(name = "COUNTY_SALES_TAX")
    private BigDecimal countySalesTax;

    @Column(name = "COUNTY_LOCAL_SALES_TAX")
    private BigDecimal countyLocalSalesTax;

    @Column(name = "COMBO_SALES_TAX")
    private BigDecimal comboSalesTax;

    @Column(name = "STATE_USE_TAX")
    private BigDecimal stateUseTax;

    @Column(name = "CITY_USE_TAX")
    private BigDecimal cityUseTax;

    @Column(name = "CITY_LOCAL_USE_TAX")
    private BigDecimal cityLocalUseTax;

    @Column(name = "COUNTY_USE_TAX")
    private BigDecimal countyUseTax;

    @Column(name = "COUNTY_LOCAL_USE_TAX")
    private BigDecimal countyLocalUseTax;

    @Column(name = "COMBO_USE_TAX")
    private BigDecimal comboUseTax;
}
