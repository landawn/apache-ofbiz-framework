package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayWorldPayEntity;

public interface PaymentGatewayWorldPayDao extends CrudDao<PaymentGatewayWorldPayEntity, String, SqlBuilder.PSC, PaymentGatewayWorldPayDao> {
}
