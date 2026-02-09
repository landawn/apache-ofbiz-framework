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
@Entity(name = "CUSTOM_SCREEN")
@Table(name = "CUSTOM_SCREEN")
public class CustomScreenEntity {
    @Id
    @Column(name = "CUSTOM_SCREEN_ID")
    private String customScreenId;

    @Column(name = "CUSTOM_SCREEN_TYPE_ID")
    private String customScreenTypeId;

    @Column(name = "CUSTOM_SCREEN_NAME")
    private String customScreenName;

    @Column(name = "CUSTOM_SCREEN_LOCATION")
    private String customScreenLocation;

    @Column(name = "DESCRIPTION")
    private String description;
}
