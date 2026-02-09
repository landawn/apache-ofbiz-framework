package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentTypeEntity;

public interface PaymentTypeDao extends CrudDao<PaymentTypeEntity, String, SQLBuilder.PSC, PaymentTypeDao> {
}
