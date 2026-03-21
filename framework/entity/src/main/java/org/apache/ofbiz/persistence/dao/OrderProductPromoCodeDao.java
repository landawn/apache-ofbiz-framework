package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderProductPromoCodeEntity;

public interface OrderProductPromoCodeDao extends CrudDao<OrderProductPromoCodeEntity, OrderProductPromoCodeEntity, SqlBuilder.PSC, OrderProductPromoCodeDao>, DelegatorQueryDao {
}
