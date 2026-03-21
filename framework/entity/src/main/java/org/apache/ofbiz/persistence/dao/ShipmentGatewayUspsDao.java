package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayUspsEntity;

public interface ShipmentGatewayUspsDao extends CrudDao<ShipmentGatewayUspsEntity, String, SqlBuilder.PSC, ShipmentGatewayUspsDao> {
}
