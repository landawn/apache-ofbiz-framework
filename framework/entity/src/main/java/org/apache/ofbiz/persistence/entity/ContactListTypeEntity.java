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
@Entity(name = "CONTACT_LIST_TYPE")
@Table(name = "CONTACT_LIST_TYPE")
public class ContactListTypeEntity {
    @Id
    @Column(name = "CONTACT_LIST_TYPE_ID")
    private String contactListTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
