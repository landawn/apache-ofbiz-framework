package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayConfigEntity;

public interface ShipmentGatewayConfigDao extends CrudDao<ShipmentGatewayConfigEntity, String, SQLBuilder.PSC, ShipmentGatewayConfigDao> {
}
