package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderSummaryEntryEntity;

public interface OrderSummaryEntryDao extends CrudDao<OrderSummaryEntryEntity, OrderSummaryEntryEntity, SqlBuilder.PSC, OrderSummaryEntryDao> {
}
