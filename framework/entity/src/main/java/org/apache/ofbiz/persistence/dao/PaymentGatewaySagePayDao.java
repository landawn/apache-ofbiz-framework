package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewaySagePayEntity;

public interface PaymentGatewaySagePayDao extends CrudDao<PaymentGatewaySagePayEntity, String, SqlBuilder.PSC, PaymentGatewaySagePayDao> {
}
