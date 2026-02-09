package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayFedexEntity;

public interface ShipmentGatewayFedexDao extends CrudDao<ShipmentGatewayFedexEntity, String, SQLBuilder.PSC, ShipmentGatewayFedexDao> {
}
