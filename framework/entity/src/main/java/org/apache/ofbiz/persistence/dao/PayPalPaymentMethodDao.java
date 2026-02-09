package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PayPalPaymentMethodEntity;

public interface PayPalPaymentMethodDao extends CrudDao<PayPalPaymentMethodEntity, String, SQLBuilder.PSC, PayPalPaymentMethodDao> {
}
