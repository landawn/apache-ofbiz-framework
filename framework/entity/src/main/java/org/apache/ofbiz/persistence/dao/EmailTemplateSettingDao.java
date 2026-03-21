package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmailTemplateSettingEntity;

public interface EmailTemplateSettingDao extends CrudDao<EmailTemplateSettingEntity, String, SqlBuilder.PSC, EmailTemplateSettingDao>, DelegatorQueryDao {
}
