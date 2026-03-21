package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderPaymentPreferenceEntity;

public interface OrderPaymentPreferenceDao extends CrudDao<OrderPaymentPreferenceEntity, String, SqlBuilder.PSC, OrderPaymentPreferenceDao>, DelegatorQueryDao {
}
