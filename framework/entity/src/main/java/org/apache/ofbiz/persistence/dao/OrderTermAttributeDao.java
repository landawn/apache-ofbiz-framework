package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderTermAttributeEntity;

public interface OrderTermAttributeDao extends CrudDao<OrderTermAttributeEntity, OrderTermAttributeEntity, SQLBuilder.PSC, OrderTermAttributeDao> {
}
