package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentRouteSegmentEntity;

public interface ShipmentRouteSegmentDao extends CrudDao<ShipmentRouteSegmentEntity, ShipmentRouteSegmentEntity, SQLBuilder.PSC, ShipmentRouteSegmentDao> , DelegatorQueryDao {
}
