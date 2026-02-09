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
@Entity(name = "GL_ACCOUNT_CATEGORY_MEMBER")
@Table(name = "GL_ACCOUNT_CATEGORY_MEMBER")
public class GlAccountCategoryMemberEntity {
    @Id
    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Id
    @Column(name = "GL_ACCOUNT_CATEGORY_ID")
    private String glAccountCategoryId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "AMOUNT_PERCENTAGE")
    private BigDecimal amountPercentage;
}
