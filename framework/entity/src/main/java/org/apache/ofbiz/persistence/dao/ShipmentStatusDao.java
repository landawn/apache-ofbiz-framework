package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentStatusEntity;

public interface ShipmentStatusDao extends CrudDao<ShipmentStatusEntity, ShipmentStatusEntity, SQLBuilder.PSC, ShipmentStatusDao> {
}
