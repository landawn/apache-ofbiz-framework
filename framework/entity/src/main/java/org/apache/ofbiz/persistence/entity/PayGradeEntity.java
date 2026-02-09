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
@Entity(name = "PAY_GRADE")
@Table(name = "PAY_GRADE")
public class PayGradeEntity {
    @Id
    @Column(name = "PAY_GRADE_ID")
    private String payGradeId;

    @Column(name = "PAY_GRADE_NAME")
    private String payGradeName;

    @Column(name = "COMMENTS")
    private String comments;
}
