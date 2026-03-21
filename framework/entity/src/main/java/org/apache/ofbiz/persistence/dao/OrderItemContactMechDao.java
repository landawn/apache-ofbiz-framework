package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemContactMechEntity;

public interface OrderItemContactMechDao extends CrudDao<OrderItemContactMechEntity, OrderItemContactMechEntity, SqlBuilder.PSC, OrderItemContactMechDao>, DelegatorQueryDao {
}
