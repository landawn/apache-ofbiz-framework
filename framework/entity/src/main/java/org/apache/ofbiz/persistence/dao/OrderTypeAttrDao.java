package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderTypeAttrEntity;

public interface OrderTypeAttrDao extends CrudDao<OrderTypeAttrEntity, OrderTypeAttrEntity, SqlBuilder.PSC, OrderTypeAttrDao> {
}
