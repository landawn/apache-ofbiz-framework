package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayUpsEntity;

public interface ShipmentGatewayUpsDao extends CrudDao<ShipmentGatewayUpsEntity, String, SqlBuilder.PSC, ShipmentGatewayUpsDao> , DelegatorQueryDao {
}
