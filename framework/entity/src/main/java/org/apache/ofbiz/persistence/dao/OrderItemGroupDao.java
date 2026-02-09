package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemGroupEntity;

public interface OrderItemGroupDao extends CrudDao<OrderItemGroupEntity, OrderItemGroupEntity, SQLBuilder.PSC, OrderItemGroupDao> {
}
