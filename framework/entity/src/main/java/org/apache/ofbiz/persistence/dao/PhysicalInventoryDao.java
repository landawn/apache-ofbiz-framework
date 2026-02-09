package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PhysicalInventoryEntity;

public interface PhysicalInventoryDao extends CrudDao<PhysicalInventoryEntity, String, SQLBuilder.PSC, PhysicalInventoryDao> {
}
