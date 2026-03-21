package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderAdjustmentEntity;

public interface OrderAdjustmentDao extends CrudDao<OrderAdjustmentEntity, String, SqlBuilder.PSC, OrderAdjustmentDao>, DelegatorQueryDao {
}
