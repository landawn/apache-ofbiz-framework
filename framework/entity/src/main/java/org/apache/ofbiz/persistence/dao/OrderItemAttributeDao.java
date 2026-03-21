package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemAttributeEntity;

public interface OrderItemAttributeDao extends CrudDao<OrderItemAttributeEntity, OrderItemAttributeEntity, SqlBuilder.PSC, OrderItemAttributeDao>, DelegatorQueryDao {
}
