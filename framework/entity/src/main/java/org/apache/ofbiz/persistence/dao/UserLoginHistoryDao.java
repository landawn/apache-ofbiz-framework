package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UserLoginHistoryEntity;

public interface UserLoginHistoryDao extends CrudDao<UserLoginHistoryEntity, UserLoginHistoryEntity, SqlBuilder.PSC, UserLoginHistoryDao> {
}
