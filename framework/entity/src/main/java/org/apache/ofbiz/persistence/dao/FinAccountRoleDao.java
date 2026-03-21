package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountRoleEntity;

public interface FinAccountRoleDao extends CrudDao<FinAccountRoleEntity, FinAccountRoleEntity, SqlBuilder.PSC, FinAccountRoleDao>, DelegatorQueryDao {
}
