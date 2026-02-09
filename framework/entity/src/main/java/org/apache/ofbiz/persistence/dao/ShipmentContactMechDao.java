package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentContactMechEntity;

public interface ShipmentContactMechDao extends CrudDao<ShipmentContactMechEntity, ShipmentContactMechEntity, SQLBuilder.PSC, ShipmentContactMechDao> {
}
