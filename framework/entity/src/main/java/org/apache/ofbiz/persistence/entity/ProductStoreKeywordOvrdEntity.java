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
@Entity(name = "PRODUCT_STORE_KEYWORD_OVRD")
@Table(name = "PRODUCT_STORE_KEYWORD_OVRD")
public class ProductStoreKeywordOvrdEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Id
    @Column(name = "KEYWORD")
    private String keyword;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "TARGET")
    private String target;

    @Column(name = "TARGET_TYPE_ENUM_ID")
    private String targetTypeEnumId;
}
