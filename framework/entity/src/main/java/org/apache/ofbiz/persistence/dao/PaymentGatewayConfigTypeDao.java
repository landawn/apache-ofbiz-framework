package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayConfigTypeEntity;

public interface PaymentGatewayConfigTypeDao extends CrudDao<PaymentGatewayConfigTypeEntity, String, SQLBuilder.PSC, PaymentGatewayConfigTypeDao> {
}
