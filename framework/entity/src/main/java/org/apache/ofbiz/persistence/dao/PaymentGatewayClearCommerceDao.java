package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayClearCommerceEntity;

public interface PaymentGatewayClearCommerceDao extends CrudDao<PaymentGatewayClearCommerceEntity, String, SqlBuilder.PSC, PaymentGatewayClearCommerceDao> {
}
