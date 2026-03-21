package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderTypeEntity;

public interface OrderTypeDao extends CrudDao<OrderTypeEntity, String, SqlBuilder.PSC, OrderTypeDao>, DelegatorQueryDao {
}
