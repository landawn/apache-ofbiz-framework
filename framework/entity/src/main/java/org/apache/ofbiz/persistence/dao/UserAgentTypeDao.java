package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UserAgentTypeEntity;

public interface UserAgentTypeDao extends CrudDao<UserAgentTypeEntity, String, SqlBuilder.PSC, UserAgentTypeDao> {
}
