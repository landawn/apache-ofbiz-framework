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
@Entity(name = "ZIP_SALES_RULE_LOOKUP")
@Table(name = "ZIP_SALES_RULE_LOOKUP")
public class ZipSalesRuleLookupEntity {
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

    @Column(name = "ID_CODE")
    private String idCode;

    @Column(name = "TAXABLE")
    private String taxable;

    @Column(name = "SHIP_COND")
    private String shipCond;
}
