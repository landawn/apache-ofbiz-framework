package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentContentTypeEntity;

public interface PaymentContentTypeDao extends CrudDao<PaymentContentTypeEntity, String, SqlBuilder.PSC, PaymentContentTypeDao> {
}
