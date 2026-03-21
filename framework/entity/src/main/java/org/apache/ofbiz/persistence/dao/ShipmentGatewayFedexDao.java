package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayFedexEntity;

public interface ShipmentGatewayFedexDao extends CrudDao<ShipmentGatewayFedexEntity, String, SqlBuilder.PSC, ShipmentGatewayFedexDao> {
}
