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
@Entity(name = "GL_ACCOUNT_CLASS")
@Table(name = "GL_ACCOUNT_CLASS")
public class GlAccountClassEntity {
    @Id
    @Column(name = "GL_ACCOUNT_CLASS_ID")
    private String glAccountClassId;

    @Column(name = "PARENT_CLASS_ID")
    private String parentClassId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_ASSET_CLASS")
    private String isAssetClass;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
