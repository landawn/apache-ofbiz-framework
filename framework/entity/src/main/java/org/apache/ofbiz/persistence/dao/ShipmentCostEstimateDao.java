package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentCostEstimateEntity;

public interface ShipmentCostEstimateDao extends CrudDao<ShipmentCostEstimateEntity, String, SQLBuilder.PSC, ShipmentCostEstimateDao> {
}
