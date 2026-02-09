package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderShipmentEntity;

public interface OrderShipmentDao extends CrudDao<OrderShipmentEntity, OrderShipmentEntity, SQLBuilder.PSC, OrderShipmentDao> {
}
