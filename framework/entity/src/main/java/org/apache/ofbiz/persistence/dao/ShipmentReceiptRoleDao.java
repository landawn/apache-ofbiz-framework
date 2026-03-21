package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentReceiptRoleEntity;

public interface ShipmentReceiptRoleDao extends CrudDao<ShipmentReceiptRoleEntity, ShipmentReceiptRoleEntity, SqlBuilder.PSC, ShipmentReceiptRoleDao> {
}
