package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemTempResEntity;

public interface InventoryItemTempResDao extends CrudDao<InventoryItemTempResEntity, InventoryItemTempResEntity, SqlBuilder.PSC, InventoryItemTempResDao> {
}
