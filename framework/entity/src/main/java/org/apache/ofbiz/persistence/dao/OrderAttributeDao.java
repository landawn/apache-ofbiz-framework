package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderAttributeEntity;

public interface OrderAttributeDao extends CrudDao<OrderAttributeEntity, OrderAttributeEntity, SQLBuilder.PSC, OrderAttributeDao> {
}
