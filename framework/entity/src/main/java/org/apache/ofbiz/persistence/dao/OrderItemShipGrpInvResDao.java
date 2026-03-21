package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemShipGrpInvResEntity;

public interface OrderItemShipGrpInvResDao extends CrudDao<OrderItemShipGrpInvResEntity, OrderItemShipGrpInvResEntity, SqlBuilder.PSC, OrderItemShipGrpInvResDao>, DelegatorQueryDao {
}
