package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentItemEntity;

public interface ShipmentItemDao extends CrudDao<ShipmentItemEntity, ShipmentItemEntity, SqlBuilder.PSC, ShipmentItemDao> {
}
