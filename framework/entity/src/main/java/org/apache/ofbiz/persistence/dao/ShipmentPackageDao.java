package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentPackageEntity;

public interface ShipmentPackageDao extends CrudDao<ShipmentPackageEntity, ShipmentPackageEntity, SqlBuilder.PSC, ShipmentPackageDao> , DelegatorQueryDao {
}
