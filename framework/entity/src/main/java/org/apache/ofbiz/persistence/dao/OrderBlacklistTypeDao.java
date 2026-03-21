package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderBlacklistTypeEntity;

public interface OrderBlacklistTypeDao extends CrudDao<OrderBlacklistTypeEntity, String, SqlBuilder.PSC, OrderBlacklistTypeDao> {
}
