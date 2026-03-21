package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentMethodTypeEntity;

public interface ShipmentMethodTypeDao extends CrudDao<ShipmentMethodTypeEntity, String, SqlBuilder.PSC, ShipmentMethodTypeDao>, DelegatorQueryDao {
}
