package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayPayPalEntity;

public interface PaymentGatewayPayPalDao extends CrudDao<PaymentGatewayPayPalEntity, String, SQLBuilder.PSC, PaymentGatewayPayPalDao> {
}
