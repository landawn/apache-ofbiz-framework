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
@Entity(name = "RETURN_ITEM_TYPE_MAP")
@Table(name = "RETURN_ITEM_TYPE_MAP")
public class ReturnItemTypeMapEntity {
    @Id
    @Column(name = "RETURN_ITEM_MAP_KEY")
    private String returnItemMapKey;

    @Id
    @Column(name = "RETURN_HEADER_TYPE_ID")
    private String returnHeaderTypeId;

    @Column(name = "RETURN_ITEM_TYPE_ID")
    private String returnItemTypeId;
}
