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
@Entity(name = "ENTITY_KEY_STORE")
@Table(name = "ENTITY_KEY_STORE")
public class EntityKeyStoreEntity {
    @Id
    @Column(name = "KEY_NAME")
    private String keyName;

    @Column(name = "KEY_TEXT")
    private String keyText;
}
