package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;

public interface UserLoginDao extends CrudDao<UserLoginEntity, String, SQLBuilder.PSC, UserLoginDao> {
}
