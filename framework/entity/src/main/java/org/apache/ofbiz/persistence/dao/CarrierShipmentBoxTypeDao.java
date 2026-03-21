package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CarrierShipmentBoxTypeEntity;

public interface CarrierShipmentBoxTypeDao extends CrudDao<CarrierShipmentBoxTypeEntity, CarrierShipmentBoxTypeEntity, SqlBuilder.PSC, CarrierShipmentBoxTypeDao> {
}
