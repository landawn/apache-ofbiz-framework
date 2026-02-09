package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayEwayEntity;

public interface PaymentGatewayEwayDao extends CrudDao<PaymentGatewayEwayEntity, String, SQLBuilder.PSC, PaymentGatewayEwayDao> {
}
