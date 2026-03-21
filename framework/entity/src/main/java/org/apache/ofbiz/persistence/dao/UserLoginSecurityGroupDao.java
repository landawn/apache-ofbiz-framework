package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UserLoginSecurityGroupEntity;

public interface UserLoginSecurityGroupDao extends CrudDao<UserLoginSecurityGroupEntity, UserLoginSecurityGroupEntity, SqlBuilder.PSC, UserLoginSecurityGroupDao> {
}
