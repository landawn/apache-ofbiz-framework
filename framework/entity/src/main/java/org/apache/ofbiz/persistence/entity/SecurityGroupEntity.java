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
@Entity(name = "SECURITY_GROUP")
@Table(name = "SECURITY_GROUP")
public class SecurityGroupEntity {
    @Id
    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "GROUP_NAME")
    private String groupName;

    @Column(name = "DESCRIPTION")
    private String description;
}
