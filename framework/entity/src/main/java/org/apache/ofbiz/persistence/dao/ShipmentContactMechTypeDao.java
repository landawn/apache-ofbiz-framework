package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentContactMechTypeEntity;

public interface ShipmentContactMechTypeDao extends CrudDao<ShipmentContactMechTypeEntity, String, SqlBuilder.PSC, ShipmentContactMechTypeDao> {
}
