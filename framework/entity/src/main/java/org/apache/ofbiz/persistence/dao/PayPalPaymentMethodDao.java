package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PayPalPaymentMethodEntity;

public interface PayPalPaymentMethodDao extends CrudDao<PayPalPaymentMethodEntity, String, SqlBuilder.PSC, PayPalPaymentMethodDao> {
}
