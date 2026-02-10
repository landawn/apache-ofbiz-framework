package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemPriceInfoEntity;

public interface OrderItemPriceInfoDao extends CrudDao<OrderItemPriceInfoEntity, String, SQLBuilder.PSC, OrderItemPriceInfoDao>, DelegatorQueryDao {
}
