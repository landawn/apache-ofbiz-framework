package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayPayflowProEntity;

public interface PaymentGatewayPayflowProDao extends CrudDao<PaymentGatewayPayflowProEntity, String, SqlBuilder.PSC, PaymentGatewayPayflowProDao> {
}
