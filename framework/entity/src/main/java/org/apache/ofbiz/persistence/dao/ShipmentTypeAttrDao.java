package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentTypeAttrEntity;

public interface ShipmentTypeAttrDao extends CrudDao<ShipmentTypeAttrEntity, ShipmentTypeAttrEntity, SqlBuilder.PSC, ShipmentTypeAttrDao> {
}
