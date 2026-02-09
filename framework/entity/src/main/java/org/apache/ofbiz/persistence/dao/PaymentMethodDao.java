package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentMethodEntity;

public interface PaymentMethodDao extends CrudDao<PaymentMethodEntity, String, SQLBuilder.PSC, PaymentMethodDao> {
}
