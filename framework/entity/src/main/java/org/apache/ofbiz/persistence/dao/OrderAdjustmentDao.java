package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderAdjustmentEntity;

public interface OrderAdjustmentDao extends CrudDao<OrderAdjustmentEntity, String, SQLBuilder.PSC, OrderAdjustmentDao>, DelegatorQueryDao {
}
