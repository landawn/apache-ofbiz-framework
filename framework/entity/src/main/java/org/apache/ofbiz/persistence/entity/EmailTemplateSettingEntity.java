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
@Entity(name = "EMAIL_TEMPLATE_SETTING")
@Table(name = "EMAIL_TEMPLATE_SETTING")
public class EmailTemplateSettingEntity {
    @Id
    @Column(name = "EMAIL_TEMPLATE_SETTING_ID")
    private String emailTemplateSettingId;

    @Column(name = "EMAIL_TYPE")
    private String emailType;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "BODY_SCREEN_LOCATION")
    private String bodyScreenLocation;

    @Column(name = "XSLFO_ATTACH_SCREEN_LOCATION")
    private String xslfoAttachScreenLocation;

    @Column(name = "FROM_ADDRESS")
    private String fromAddress;

    @Column(name = "CC_ADDRESS")
    private String ccAddress;

    @Column(name = "BCC_ADDRESS")
    private String bccAddress;

    @Column(name = "SUBJECT")
    private String subject;

    @Column(name = "CONTENT_TYPE")
    private String contentType;
}
