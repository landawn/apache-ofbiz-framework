package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemAttributeEntity;

public interface OrderItemAttributeDao extends CrudDao<OrderItemAttributeEntity, OrderItemAttributeEntity, SQLBuilder.PSC, OrderItemAttributeDao>, DelegatorQueryDao {
}
