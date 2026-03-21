package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderBlacklistEntity;

public interface OrderBlacklistDao extends CrudDao<OrderBlacklistEntity, OrderBlacklistEntity, SqlBuilder.PSC, OrderBlacklistDao> {
}
