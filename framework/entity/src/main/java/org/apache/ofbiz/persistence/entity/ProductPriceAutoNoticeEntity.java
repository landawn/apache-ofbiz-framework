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
@Entity(name = "PRODUCT_PRICE_AUTO_NOTICE")
@Table(name = "PRODUCT_PRICE_AUTO_NOTICE")
public class ProductPriceAutoNoticeEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_NOTICE_ID")
    private String productPriceNoticeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "RUN_DATE")
    private Timestamp runDate;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
