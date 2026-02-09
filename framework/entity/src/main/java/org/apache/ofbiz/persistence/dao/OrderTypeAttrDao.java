package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderTypeAttrEntity;

public interface OrderTypeAttrDao extends CrudDao<OrderTypeAttrEntity, OrderTypeAttrEntity, SQLBuilder.PSC, OrderTypeAttrDao> {
}
