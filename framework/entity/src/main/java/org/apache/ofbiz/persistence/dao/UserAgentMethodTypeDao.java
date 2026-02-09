package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UserAgentMethodTypeEntity;

public interface UserAgentMethodTypeDao extends CrudDao<UserAgentMethodTypeEntity, String, SQLBuilder.PSC, UserAgentMethodTypeDao> {
}
