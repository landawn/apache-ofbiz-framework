package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderPaymentPreferenceEntity;

public interface OrderPaymentPreferenceDao extends CrudDao<OrderPaymentPreferenceEntity, String, SQLBuilder.PSC, OrderPaymentPreferenceDao> {
}
