package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemRoleEntity;

public interface OrderItemRoleDao extends CrudDao<OrderItemRoleEntity, OrderItemRoleEntity, SqlBuilder.PSC, OrderItemRoleDao>, DelegatorQueryDao {
}
