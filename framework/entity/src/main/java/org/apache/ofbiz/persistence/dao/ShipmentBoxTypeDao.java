package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentBoxTypeEntity;

public interface ShipmentBoxTypeDao extends CrudDao<ShipmentBoxTypeEntity, String, SqlBuilder.PSC, ShipmentBoxTypeDao> {
}
