package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemVarianceEntity;

public interface InventoryItemVarianceDao extends CrudDao<InventoryItemVarianceEntity, InventoryItemVarianceEntity, SqlBuilder.PSC, InventoryItemVarianceDao> {
}
