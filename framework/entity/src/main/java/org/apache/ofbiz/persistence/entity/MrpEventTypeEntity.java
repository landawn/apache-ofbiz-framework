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
@Entity(name = "MRP_EVENT_TYPE")
@Table(name = "MRP_EVENT_TYPE")
public class MrpEventTypeEntity {
    @Id
    @Column(name = "MRP_EVENT_TYPE_ID")
    private String mrpEventTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
