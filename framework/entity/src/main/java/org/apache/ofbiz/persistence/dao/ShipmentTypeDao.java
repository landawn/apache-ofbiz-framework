package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentTypeEntity;

public interface ShipmentTypeDao extends CrudDao<ShipmentTypeEntity, String, SQLBuilder.PSC, ShipmentTypeDao> {
}
