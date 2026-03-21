package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemAttributeEntity;

public interface InventoryItemAttributeDao extends CrudDao<InventoryItemAttributeEntity, InventoryItemAttributeEntity, SqlBuilder.PSC, InventoryItemAttributeDao> {
}
