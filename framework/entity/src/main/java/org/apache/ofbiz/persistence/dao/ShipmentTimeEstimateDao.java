package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentTimeEstimateEntity;

public interface ShipmentTimeEstimateDao extends CrudDao<ShipmentTimeEstimateEntity, ShipmentTimeEstimateEntity, SqlBuilder.PSC, ShipmentTimeEstimateDao> {
}
