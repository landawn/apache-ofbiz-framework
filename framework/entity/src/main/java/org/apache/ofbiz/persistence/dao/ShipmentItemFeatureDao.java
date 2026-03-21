package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentItemFeatureEntity;

public interface ShipmentItemFeatureDao extends CrudDao<ShipmentItemFeatureEntity, ShipmentItemFeatureEntity, SqlBuilder.PSC, ShipmentItemFeatureDao> {
}
