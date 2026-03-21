package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UserPrefGroupTypeEntity;

public interface UserPrefGroupTypeDao extends CrudDao<UserPrefGroupTypeEntity, String, SqlBuilder.PSC, UserPrefGroupTypeDao> {
}
