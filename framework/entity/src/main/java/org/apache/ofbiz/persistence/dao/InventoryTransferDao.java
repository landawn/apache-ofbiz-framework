package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryTransferEntity;

public interface InventoryTransferDao extends CrudDao<InventoryTransferEntity, String, SqlBuilder.PSC, InventoryTransferDao> {
}
