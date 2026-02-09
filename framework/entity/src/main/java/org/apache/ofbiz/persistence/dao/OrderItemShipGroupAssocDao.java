package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemShipGroupAssocEntity;

public interface OrderItemShipGroupAssocDao extends CrudDao<OrderItemShipGroupAssocEntity, OrderItemShipGroupAssocEntity, SQLBuilder.PSC, OrderItemShipGroupAssocDao> {
}
