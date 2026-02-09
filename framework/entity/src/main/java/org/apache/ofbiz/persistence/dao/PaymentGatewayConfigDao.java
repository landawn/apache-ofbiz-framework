package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayConfigEntity;

public interface PaymentGatewayConfigDao extends CrudDao<PaymentGatewayConfigEntity, String, SQLBuilder.PSC, PaymentGatewayConfigDao> {
}
