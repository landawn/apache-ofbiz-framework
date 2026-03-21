package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderDenylistEntity;

public interface OrderDenylistDao extends CrudDao<OrderDenylistEntity, OrderDenylistEntity, SqlBuilder.PSC, OrderDenylistDao> {
}
