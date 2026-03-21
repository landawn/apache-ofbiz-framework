package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentStatusEntity;

public interface ShipmentStatusDao extends CrudDao<ShipmentStatusEntity, ShipmentStatusEntity, SqlBuilder.PSC, ShipmentStatusDao> {
}
