package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemChangeEntity;

public interface OrderItemChangeDao extends CrudDao<OrderItemChangeEntity, String, SqlBuilder.PSC, OrderItemChangeDao>, DelegatorQueryDao {
}
