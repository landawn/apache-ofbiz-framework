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
@Entity(name = "MIME_TYPE")
@Table(name = "MIME_TYPE")
public class MimeTypeEntity {
    @Id
    @Column(name = "MIME_TYPE_ID")
    private String mimeTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
