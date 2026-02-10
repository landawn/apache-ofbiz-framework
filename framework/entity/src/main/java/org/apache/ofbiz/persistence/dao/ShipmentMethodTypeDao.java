package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentMethodTypeEntity;

public interface ShipmentMethodTypeDao extends CrudDao<ShipmentMethodTypeEntity, String, SQLBuilder.PSC, ShipmentMethodTypeDao>, DelegatorQueryDao {
}
