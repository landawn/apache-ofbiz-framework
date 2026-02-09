package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemAttributeEntity;

public interface InventoryItemAttributeDao extends CrudDao<InventoryItemAttributeEntity, InventoryItemAttributeEntity, SQLBuilder.PSC, InventoryItemAttributeDao> {
}
