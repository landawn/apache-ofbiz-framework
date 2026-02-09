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
@Entity(name = "FILE_EXTENSION")
@Table(name = "FILE_EXTENSION")
public class FileExtensionEntity {
    @Id
    @Column(name = "FILE_EXTENSION_ID")
    private String fileExtensionId;

    @Column(name = "MIME_TYPE_ID")
    private String mimeTypeId;
}
