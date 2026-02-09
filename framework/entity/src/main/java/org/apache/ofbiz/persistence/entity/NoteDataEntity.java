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
@Entity(name = "NOTE_DATA")
@Table(name = "NOTE_DATA")
public class NoteDataEntity {
    @Id
    @Column(name = "NOTE_ID")
    private String noteId;

    @Column(name = "NOTE_NAME")
    private String noteName;

    @Column(name = "NOTE_INFO")
    private String noteInfo;

    @Column(name = "NOTE_DATE_TIME")
    private Timestamp noteDateTime;

    @Column(name = "MORE_INFO_URL")
    private String moreInfoUrl;

    @Column(name = "MORE_INFO_ITEM_ID")
    private String moreInfoItemId;

    @Column(name = "MORE_INFO_ITEM_NAME")
    private String moreInfoItemName;

    @Column(name = "NOTE_PARTY")
    private String noteParty;
}
