package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemShipGrpInvResEntity;

public interface OrderItemAndShipGrpInvResAndItemDao
        extends CrudDao<OrderItemShipGrpInvResEntity, OrderItemShipGrpInvResEntity, SQLBuilder.PSC, OrderItemAndShipGrpInvResAndItemDao>,
        DelegatorQueryDao {
}
