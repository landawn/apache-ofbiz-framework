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
@Entity(name = "STANDARD_LANGUAGE")
@Table(name = "STANDARD_LANGUAGE")
public class StandardLanguageEntity {
    @Id
    @Column(name = "STANDARD_LANGUAGE_ID")
    private String standardLanguageId;

    @Column(name = "LANG_CODE3T")
    private String langCode3t;

    @Column(name = "LANG_CODE3B")
    private String langCode3b;

    @Column(name = "LANG_CODE2")
    private String langCode2;

    @Column(name = "LANG_NAME")
    private String langName;

    @Column(name = "LANG_FAMILY")
    private String langFamily;

    @Column(name = "LANG_CHARSET")
    private String langCharset;
}
