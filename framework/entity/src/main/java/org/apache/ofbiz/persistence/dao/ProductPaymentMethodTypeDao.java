package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPaymentMethodTypeEntity;

public interface ProductPaymentMethodTypeDao extends CrudDao<ProductPaymentMethodTypeEntity, ProductPaymentMethodTypeEntity, SqlBuilder.PSC, ProductPaymentMethodTypeDao> {
}
