package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayWorldPayEntity;

public interface PaymentGatewayWorldPayDao extends CrudDao<PaymentGatewayWorldPayEntity, String, SQLBuilder.PSC, PaymentGatewayWorldPayDao> {
}
