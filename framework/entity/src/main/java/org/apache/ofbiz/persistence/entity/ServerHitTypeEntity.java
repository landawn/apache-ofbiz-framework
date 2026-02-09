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
@Entity(name = "SERVER_HIT_TYPE")
@Table(name = "SERVER_HIT_TYPE")
public class ServerHitTypeEntity {
    @Id
    @Column(name = "HIT_TYPE_ID")
    private String hitTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
