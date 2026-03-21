package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemEntity;

public interface OrderItemQuantityReportGroupByProductDao
        extends CrudDao<OrderItemEntity, OrderItemEntity, SqlBuilder.PSC, OrderItemQuantityReportGroupByProductDao>,
        DelegatorQueryDao {
}
