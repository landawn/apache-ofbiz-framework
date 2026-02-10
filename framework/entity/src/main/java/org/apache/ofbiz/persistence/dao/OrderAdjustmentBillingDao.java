package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderAdjustmentBillingEntity;

public interface OrderAdjustmentBillingDao extends CrudDao<OrderAdjustmentBillingEntity, OrderAdjustmentBillingEntity, SQLBuilder.PSC, OrderAdjustmentBillingDao>, DelegatorQueryDao {
}
