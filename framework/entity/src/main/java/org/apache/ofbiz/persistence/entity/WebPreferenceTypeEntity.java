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
@Entity(name = "WEB_PREFERENCE_TYPE")
@Table(name = "WEB_PREFERENCE_TYPE")
public class WebPreferenceTypeEntity {
    @Id
    @Column(name = "WEB_PREFERENCE_TYPE_ID")
    private String webPreferenceTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
