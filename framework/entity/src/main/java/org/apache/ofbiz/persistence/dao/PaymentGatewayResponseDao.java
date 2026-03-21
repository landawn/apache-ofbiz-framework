package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayResponseEntity;

public interface PaymentGatewayResponseDao extends CrudDao<PaymentGatewayResponseEntity, String, SqlBuilder.PSC, PaymentGatewayResponseDao> {
}
