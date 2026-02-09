package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentEntity;

public interface PaymentDao extends CrudDao<PaymentEntity, String, SQLBuilder.PSC, PaymentDao> {
}
