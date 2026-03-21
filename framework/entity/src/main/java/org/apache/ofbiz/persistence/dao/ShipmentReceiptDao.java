package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentReceiptEntity;

public interface ShipmentReceiptDao extends CrudDao<ShipmentReceiptEntity, String, SqlBuilder.PSC, ShipmentReceiptDao>, DelegatorQueryDao {
}
