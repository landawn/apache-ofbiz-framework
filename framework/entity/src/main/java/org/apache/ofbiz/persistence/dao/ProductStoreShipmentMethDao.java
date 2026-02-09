package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreShipmentMethEntity;

public interface ProductStoreShipmentMethDao extends CrudDao<ProductStoreShipmentMethEntity, String, SQLBuilder.PSC, ProductStoreShipmentMethDao> {
}
