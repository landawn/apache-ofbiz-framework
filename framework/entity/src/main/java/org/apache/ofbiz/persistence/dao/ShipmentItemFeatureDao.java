package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentItemFeatureEntity;

public interface ShipmentItemFeatureDao extends CrudDao<ShipmentItemFeatureEntity, ShipmentItemFeatureEntity, SQLBuilder.PSC, ShipmentItemFeatureDao> {
}
