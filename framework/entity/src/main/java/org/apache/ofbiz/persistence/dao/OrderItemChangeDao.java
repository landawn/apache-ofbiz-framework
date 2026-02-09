package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemChangeEntity;

public interface OrderItemChangeDao extends CrudDao<OrderItemChangeEntity, String, SQLBuilder.PSC, OrderItemChangeDao> {
}
