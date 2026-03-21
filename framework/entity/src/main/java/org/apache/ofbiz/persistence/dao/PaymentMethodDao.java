package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentMethodEntity;

public interface PaymentMethodDao extends CrudDao<PaymentMethodEntity, String, SqlBuilder.PSC, PaymentMethodDao>, DelegatorQueryDao {
}
