package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DeliveryEntity;

public interface DeliveryDao extends CrudDao<DeliveryEntity, String, SqlBuilder.PSC, DeliveryDao> {
}
