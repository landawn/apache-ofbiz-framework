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
@Entity(name = "AUDIO_DATA_RESOURCE")
@Table(name = "AUDIO_DATA_RESOURCE")
public class AudioDataResourceEntity {
    @Id
    @Column(name = "DATA_RESOURCE_ID")
    private String dataResourceId;

    @Column(name = "AUDIO_DATA")
    private byte[] audioData;
}
