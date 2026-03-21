package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemShipGroupAssocEntity;

public interface OrderItemShipGroupAssocDao extends CrudDao<OrderItemShipGroupAssocEntity, OrderItemShipGroupAssocEntity, SqlBuilder.PSC, OrderItemShipGroupAssocDao>, DelegatorQueryDao {
}
