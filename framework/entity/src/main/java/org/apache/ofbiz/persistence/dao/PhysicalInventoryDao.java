package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PhysicalInventoryEntity;

public interface PhysicalInventoryDao extends CrudDao<PhysicalInventoryEntity, String, SqlBuilder.PSC, PhysicalInventoryDao> {
}
