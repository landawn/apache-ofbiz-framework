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
@Entity(name = "CHARACTER_SET")
@Table(name = "CHARACTER_SET")
public class CharacterSetEntity {
    @Id
    @Column(name = "CHARACTER_SET_ID")
    private String characterSetId;

    @Column(name = "DESCRIPTION")
    private String description;
}
