package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemAssocEntity;

public interface OrderItemAssocDao extends CrudDao<OrderItemAssocEntity, OrderItemAssocEntity, SqlBuilder.PSC, OrderItemAssocDao>, DelegatorQueryDao {
}
