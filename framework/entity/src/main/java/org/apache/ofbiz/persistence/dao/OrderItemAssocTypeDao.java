package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemAssocTypeEntity;

public interface OrderItemAssocTypeDao extends CrudDao<OrderItemAssocTypeEntity, String, SqlBuilder.PSC, OrderItemAssocTypeDao> {
}
