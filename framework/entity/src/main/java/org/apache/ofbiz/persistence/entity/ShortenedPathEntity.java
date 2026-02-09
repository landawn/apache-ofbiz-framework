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
@Entity(name = "SHORTENED_PATH")
@Table(name = "SHORTENED_PATH")
public class ShortenedPathEntity {
    @Id
    @Column(name = "SHORTENED_PATH")
    private String shortenedPath;

    @Column(name = "ORIGINAL_PATH_HASH")
    private String originalPathHash;

    @Column(name = "ORIGINAL_PATH")
    private String originalPath;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;
}
