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
@Entity(name = "NEED_TYPE")
@Table(name = "NEED_TYPE")
public class NeedTypeEntity {
    @Id
    @Column(name = "NEED_TYPE_ID")
    private String needTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
