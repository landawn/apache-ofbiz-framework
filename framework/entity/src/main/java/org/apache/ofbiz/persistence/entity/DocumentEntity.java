package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "DOCUMENT")
@Table(name = "DOCUMENT")
public class DocumentEntity {
    @Id
    @Column(name = "DOCUMENT_ID")
    private String documentId;

    @Column(name = "DOCUMENT_TYPE_ID")
    private String documentTypeId;

    @Column(name = "DATE_CREATED")
    private Timestamp dateCreated;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "DOCUMENT_LOCATION")
    private String documentLocation;

    @Column(name = "DOCUMENT_TEXT")
    private String documentText;

    @Column(name = "IMAGE_DATA")
    private byte[] imageData;
}
