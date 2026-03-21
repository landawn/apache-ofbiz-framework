package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemEntity;

public interface OrderItemDao extends CrudDao<OrderItemEntity, OrderItemEntity, SqlBuilder.PSC, OrderItemDao>, DelegatorQueryDao {
}
