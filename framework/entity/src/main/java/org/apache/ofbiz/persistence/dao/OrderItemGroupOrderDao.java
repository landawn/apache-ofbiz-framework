package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemGroupOrderEntity;

public interface OrderItemGroupOrderDao extends CrudDao<OrderItemGroupOrderEntity, OrderItemGroupOrderEntity, SQLBuilder.PSC, OrderItemGroupOrderDao> {
}
