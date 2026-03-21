package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentTypeAttrEntity;

public interface PaymentTypeAttrDao extends CrudDao<PaymentTypeAttrEntity, PaymentTypeAttrEntity, SqlBuilder.PSC, PaymentTypeAttrDao> {
}
