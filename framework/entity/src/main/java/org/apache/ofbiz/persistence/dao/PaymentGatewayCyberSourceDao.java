package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayCyberSourceEntity;

public interface PaymentGatewayCyberSourceDao extends CrudDao<PaymentGatewayCyberSourceEntity, String, SqlBuilder.PSC, PaymentGatewayCyberSourceDao> {
}
