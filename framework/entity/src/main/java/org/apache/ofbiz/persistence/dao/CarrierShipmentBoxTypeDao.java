package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CarrierShipmentBoxTypeEntity;

public interface CarrierShipmentBoxTypeDao extends CrudDao<CarrierShipmentBoxTypeEntity, CarrierShipmentBoxTypeEntity, SQLBuilder.PSC, CarrierShipmentBoxTypeDao> {
}
