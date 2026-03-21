package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InventoryItemDetailEntity;

public interface InventoryItemDetailDao extends CrudDao<InventoryItemDetailEntity, InventoryItemDetailEntity, SqlBuilder.PSC, InventoryItemDetailDao> , DelegatorQueryDao{
}
