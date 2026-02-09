package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreVendorShipmentEntity;

public interface ProductStoreVendorShipmentDao extends CrudDao<ProductStoreVendorShipmentEntity, ProductStoreVendorShipmentEntity, SQLBuilder.PSC, ProductStoreVendorShipmentDao> {
}
