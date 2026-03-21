package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentEntity;

public interface PaymentDao extends CrudDao<PaymentEntity, String, SqlBuilder.PSC, PaymentDao> {
}
