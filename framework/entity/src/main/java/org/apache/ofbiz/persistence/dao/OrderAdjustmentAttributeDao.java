package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderAdjustmentAttributeEntity;

public interface OrderAdjustmentAttributeDao extends CrudDao<OrderAdjustmentAttributeEntity, OrderAdjustmentAttributeEntity, SQLBuilder.PSC, OrderAdjustmentAttributeDao> {
}
