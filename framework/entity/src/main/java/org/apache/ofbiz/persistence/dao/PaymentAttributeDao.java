package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentAttributeEntity;

public interface PaymentAttributeDao extends CrudDao<PaymentAttributeEntity, PaymentAttributeEntity, SqlBuilder.PSC, PaymentAttributeDao> {
}
