package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentAttributeEntity;

public interface ShipmentAttributeDao extends CrudDao<ShipmentAttributeEntity, ShipmentAttributeEntity, SqlBuilder.PSC, ShipmentAttributeDao> {
}
