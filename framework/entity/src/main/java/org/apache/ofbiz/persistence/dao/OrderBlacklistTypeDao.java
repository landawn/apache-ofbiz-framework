package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderBlacklistTypeEntity;

public interface OrderBlacklistTypeDao extends CrudDao<OrderBlacklistTypeEntity, String, SQLBuilder.PSC, OrderBlacklistTypeDao> {
}
