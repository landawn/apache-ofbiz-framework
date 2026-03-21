package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentPackageContentEntity;

public interface ShipmentPackageContentDao extends CrudDao<ShipmentPackageContentEntity, ShipmentPackageContentEntity, SqlBuilder.PSC, ShipmentPackageContentDao> {
}
