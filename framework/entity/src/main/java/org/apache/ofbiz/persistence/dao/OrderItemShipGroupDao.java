package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemShipGroupEntity;

public interface OrderItemShipGroupDao extends CrudDao<OrderItemShipGroupEntity, OrderItemShipGroupEntity, SqlBuilder.PSC, OrderItemShipGroupDao>, DelegatorQueryDao {
}
