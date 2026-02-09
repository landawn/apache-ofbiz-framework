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
@Entity(name = "STATUS_VALID_CHANGE")
@Table(name = "STATUS_VALID_CHANGE")
public class StatusValidChangeEntity {
    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "STATUS_ID_TO")
    private String statusIdTo;

    @Column(name = "CONDITION_EXPRESSION")
    private String conditionExpression;

    @Column(name = "TRANSITION_NAME")
    private String transitionName;
}
