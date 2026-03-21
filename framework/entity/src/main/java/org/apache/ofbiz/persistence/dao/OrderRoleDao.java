package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderRoleEntity;

public interface OrderRoleDao extends CrudDao<OrderRoleEntity, OrderRoleEntity, SqlBuilder.PSC, OrderRoleDao>, DelegatorQueryDao {
}
