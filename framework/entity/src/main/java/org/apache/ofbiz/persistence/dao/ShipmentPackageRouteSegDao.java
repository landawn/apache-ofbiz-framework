package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentPackageRouteSegEntity;

public interface ShipmentPackageRouteSegDao extends CrudDao<ShipmentPackageRouteSegEntity, ShipmentPackageRouteSegEntity, SQLBuilder.PSC, ShipmentPackageRouteSegDao> , DelegatorQueryDao {
}
