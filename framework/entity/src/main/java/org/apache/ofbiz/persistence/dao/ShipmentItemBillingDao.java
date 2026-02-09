package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShipmentItemBillingEntity;

public interface ShipmentItemBillingDao extends CrudDao<ShipmentItemBillingEntity, ShipmentItemBillingEntity, SQLBuilder.PSC, ShipmentItemBillingDao> {
}
