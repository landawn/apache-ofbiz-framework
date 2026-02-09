package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentPackageEntity;

public interface ShipmentPackageDao extends CrudDao<ShipmentPackageEntity, ShipmentPackageEntity, SQLBuilder.PSC, ShipmentPackageDao> {
}
