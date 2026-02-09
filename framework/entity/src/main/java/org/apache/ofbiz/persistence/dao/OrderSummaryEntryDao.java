package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderSummaryEntryEntity;

public interface OrderSummaryEntryDao extends CrudDao<OrderSummaryEntryEntity, OrderSummaryEntryEntity, SQLBuilder.PSC, OrderSummaryEntryDao> {
}
