package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPaymentMethodTypeEntity;

public interface ProductPaymentMethodTypeDao extends CrudDao<ProductPaymentMethodTypeEntity, ProductPaymentMethodTypeEntity, SQLBuilder.PSC, ProductPaymentMethodTypeDao> {
}
