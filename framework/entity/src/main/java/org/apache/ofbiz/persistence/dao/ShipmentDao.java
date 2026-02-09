package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentEntity;

public interface ShipmentDao extends CrudDao<ShipmentEntity, String, SQLBuilder.PSC, ShipmentDao> {
}
