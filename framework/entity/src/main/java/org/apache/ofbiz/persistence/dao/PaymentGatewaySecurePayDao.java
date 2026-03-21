package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewaySecurePayEntity;

public interface PaymentGatewaySecurePayDao extends CrudDao<PaymentGatewaySecurePayEntity, String, SqlBuilder.PSC, PaymentGatewaySecurePayDao> {
}
