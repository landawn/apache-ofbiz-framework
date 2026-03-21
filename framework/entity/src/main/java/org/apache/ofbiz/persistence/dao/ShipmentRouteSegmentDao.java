package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentRouteSegmentEntity;

public interface ShipmentRouteSegmentDao extends CrudDao<ShipmentRouteSegmentEntity, ShipmentRouteSegmentEntity, SqlBuilder.PSC, ShipmentRouteSegmentDao> , DelegatorQueryDao {
}
