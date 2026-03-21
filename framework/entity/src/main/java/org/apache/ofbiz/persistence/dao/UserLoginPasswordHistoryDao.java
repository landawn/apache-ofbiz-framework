package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UserLoginPasswordHistoryEntity;

public interface UserLoginPasswordHistoryDao extends CrudDao<UserLoginPasswordHistoryEntity, UserLoginPasswordHistoryEntity, SqlBuilder.PSC, UserLoginPasswordHistoryDao> {
}
