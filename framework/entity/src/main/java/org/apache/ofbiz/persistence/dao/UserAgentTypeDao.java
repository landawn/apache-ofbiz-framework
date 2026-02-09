package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UserAgentTypeEntity;

public interface UserAgentTypeDao extends CrudDao<UserAgentTypeEntity, String, SQLBuilder.PSC, UserAgentTypeDao> {
}
