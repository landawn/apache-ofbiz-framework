package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderContentEntity;

public interface OrderContentDao extends CrudDao<OrderContentEntity, OrderContentEntity, SqlBuilder.PSC, OrderContentDao> {
}
