package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayDhlEntity;

public interface ShipmentGatewayDhlDao extends CrudDao<ShipmentGatewayDhlEntity, String, SQLBuilder.PSC, ShipmentGatewayDhlDao> {
}
