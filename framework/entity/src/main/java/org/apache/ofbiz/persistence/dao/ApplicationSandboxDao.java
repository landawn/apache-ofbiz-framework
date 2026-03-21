package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ApplicationSandboxEntity;

public interface ApplicationSandboxDao extends CrudDao<ApplicationSandboxEntity, String, SqlBuilder.PSC, ApplicationSandboxDao> {
}
