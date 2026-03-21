package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentMethodTypeEntity;

public interface PaymentMethodTypeDao extends CrudDao<PaymentMethodTypeEntity, String, SqlBuilder.PSC, PaymentMethodTypeDao>, DelegatorQueryDao {
}
