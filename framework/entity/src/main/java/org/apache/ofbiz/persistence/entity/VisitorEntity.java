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
@Entity(name = "VISITOR")
@Table(name = "VISITOR")
public class VisitorEntity {
    @Id
    @Column(name = "VISITOR_ID")
    private String visitorId;

    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "PARTY_ID")
    private String partyId;
}
