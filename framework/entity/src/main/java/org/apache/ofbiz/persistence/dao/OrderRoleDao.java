package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderRoleEntity;

public interface OrderRoleDao extends CrudDao<OrderRoleEntity, OrderRoleEntity, SQLBuilder.PSC, OrderRoleDao>, DelegatorQueryDao {
}
