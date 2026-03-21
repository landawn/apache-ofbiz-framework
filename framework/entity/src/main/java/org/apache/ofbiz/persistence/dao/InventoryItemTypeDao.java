package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemTypeEntity;

public interface InventoryItemTypeDao extends CrudDao<InventoryItemTypeEntity, String, SqlBuilder.PSC, InventoryItemTypeDao> {
}
