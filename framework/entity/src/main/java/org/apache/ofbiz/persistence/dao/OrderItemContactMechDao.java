package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemContactMechEntity;

public interface OrderItemContactMechDao extends CrudDao<OrderItemContactMechEntity, OrderItemContactMechEntity, SQLBuilder.PSC, OrderItemContactMechDao>, DelegatorQueryDao {
}
