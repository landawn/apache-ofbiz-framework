package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemEntity;

public interface InventoryItemDao extends CrudDao<InventoryItemEntity, String, SQLBuilder.PSC, InventoryItemDao> {
}
