package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderDenylistTypeEntity;

public interface OrderDenylistTypeDao extends CrudDao<OrderDenylistTypeEntity, String, SqlBuilder.PSC, OrderDenylistTypeDao> {
}
