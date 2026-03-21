package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderAdjustmentTypeAttrEntity;

public interface OrderAdjustmentTypeAttrDao extends CrudDao<OrderAdjustmentTypeAttrEntity, OrderAdjustmentTypeAttrEntity, SqlBuilder.PSC, OrderAdjustmentTypeAttrDao> {
}
