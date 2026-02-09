package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayRespMsgEntity;

public interface PaymentGatewayRespMsgDao extends CrudDao<PaymentGatewayRespMsgEntity, String, SQLBuilder.PSC, PaymentGatewayRespMsgDao> {
}
