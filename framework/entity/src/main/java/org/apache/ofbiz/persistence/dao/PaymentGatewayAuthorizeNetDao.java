package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayAuthorizeNetEntity;

public interface PaymentGatewayAuthorizeNetDao extends CrudDao<PaymentGatewayAuthorizeNetEntity, String, SQLBuilder.PSC, PaymentGatewayAuthorizeNetDao> {
}
