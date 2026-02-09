package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DeliveryEntity;

public interface DeliveryDao extends CrudDao<DeliveryEntity, String, SQLBuilder.PSC, DeliveryDao> {
}
