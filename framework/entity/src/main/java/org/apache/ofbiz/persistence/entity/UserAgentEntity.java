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
@Entity(name = "USER_AGENT")
@Table(name = "USER_AGENT")
public class UserAgentEntity {
    @Id
    @Column(name = "USER_AGENT_ID")
    private String userAgentId;

    @Column(name = "BROWSER_TYPE_ID")
    private String browserTypeId;

    @Column(name = "PLATFORM_TYPE_ID")
    private String platformTypeId;

    @Column(name = "PROTOCOL_TYPE_ID")
    private String protocolTypeId;

    @Column(name = "USER_AGENT_TYPE_ID")
    private String userAgentTypeId;

    @Column(name = "USER_AGENT_METHOD_TYPE_ID")
    private String userAgentMethodTypeId;
}
