package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayConfigTypeEntity;

public interface ShipmentGatewayConfigTypeDao extends CrudDao<ShipmentGatewayConfigTypeEntity, String, SQLBuilder.PSC, ShipmentGatewayConfigTypeDao> {
}
