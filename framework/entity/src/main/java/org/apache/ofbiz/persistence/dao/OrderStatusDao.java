package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderStatusEntity;

public interface OrderStatusDao extends CrudDao<OrderStatusEntity, String, SqlBuilder.PSC, OrderStatusDao>, DelegatorQueryDao {
}
