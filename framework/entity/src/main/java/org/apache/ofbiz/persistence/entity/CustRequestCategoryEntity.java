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
@Entity(name = "CUST_REQUEST_CATEGORY")
@Table(name = "CUST_REQUEST_CATEGORY")
public class CustRequestCategoryEntity {
    @Id
    @Column(name = "CUST_REQUEST_CATEGORY_ID")
    private String custRequestCategoryId;

    @Column(name = "CUST_REQUEST_TYPE_ID")
    private String custRequestTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
