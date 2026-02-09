package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderHeaderEntity;

public interface OrderHeaderDao extends CrudDao<OrderHeaderEntity, String, SQLBuilder.PSC, OrderHeaderDao> {
}
