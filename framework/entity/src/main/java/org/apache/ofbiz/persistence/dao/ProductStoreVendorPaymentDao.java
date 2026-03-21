package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreVendorPaymentEntity;

public interface ProductStoreVendorPaymentDao extends CrudDao<ProductStoreVendorPaymentEntity, ProductStoreVendorPaymentEntity, SqlBuilder.PSC, ProductStoreVendorPaymentDao> {
}
