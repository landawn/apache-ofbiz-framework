package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemPriceInfoEntity;

public interface OrderItemPriceInfoDao extends CrudDao<OrderItemPriceInfoEntity, String, SqlBuilder.PSC, OrderItemPriceInfoDao>, DelegatorQueryDao {
}
