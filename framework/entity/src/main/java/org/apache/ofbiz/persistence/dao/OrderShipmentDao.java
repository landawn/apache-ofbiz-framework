package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderShipmentEntity;

public interface OrderShipmentDao extends CrudDao<OrderShipmentEntity, OrderShipmentEntity, SqlBuilder.PSC, OrderShipmentDao>, DelegatorQueryDao {
}
