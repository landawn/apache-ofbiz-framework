package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmailTemplateSettingEntity;

public interface EmailTemplateSettingDao extends CrudDao<EmailTemplateSettingEntity, String, SQLBuilder.PSC, EmailTemplateSettingDao> {
}
