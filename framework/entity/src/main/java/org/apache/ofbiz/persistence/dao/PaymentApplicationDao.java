package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentApplicationEntity;

public interface PaymentApplicationDao extends CrudDao<PaymentApplicationEntity, String, SqlBuilder.PSC, PaymentApplicationDao> {
}
