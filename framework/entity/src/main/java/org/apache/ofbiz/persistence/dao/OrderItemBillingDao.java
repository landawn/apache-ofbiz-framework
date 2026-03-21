package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemBillingEntity;

public interface OrderItemBillingDao extends CrudDao<OrderItemBillingEntity, OrderItemBillingEntity, SqlBuilder.PSC, OrderItemBillingDao>, DelegatorQueryDao {
}
