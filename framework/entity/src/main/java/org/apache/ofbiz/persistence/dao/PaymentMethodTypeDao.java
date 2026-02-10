package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentMethodTypeEntity;

public interface PaymentMethodTypeDao extends CrudDao<PaymentMethodTypeEntity, String, SQLBuilder.PSC, PaymentMethodTypeDao>, DelegatorQueryDao {
}
