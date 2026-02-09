package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemTypeAttrEntity;

public interface InventoryItemTypeAttrDao extends CrudDao<InventoryItemTypeAttrEntity, InventoryItemTypeAttrEntity, SQLBuilder.PSC, InventoryItemTypeAttrDao> {
}
