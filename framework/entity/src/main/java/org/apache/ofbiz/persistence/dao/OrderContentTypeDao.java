package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderContentTypeEntity;

public interface OrderContentTypeDao extends CrudDao<OrderContentTypeEntity, String, SQLBuilder.PSC, OrderContentTypeDao> {
}
