package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentPackageRouteSegEntity;

public interface ShipmentPackageRouteSegDao extends CrudDao<ShipmentPackageRouteSegEntity, ShipmentPackageRouteSegEntity, SqlBuilder.PSC, ShipmentPackageRouteSegDao> , DelegatorQueryDao {
}
