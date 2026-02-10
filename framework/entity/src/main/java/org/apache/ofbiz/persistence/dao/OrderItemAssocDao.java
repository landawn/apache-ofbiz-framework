package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemAssocEntity;

public interface OrderItemAssocDao extends CrudDao<OrderItemAssocEntity, OrderItemAssocEntity, SQLBuilder.PSC, OrderItemAssocDao>, DelegatorQueryDao {
}
