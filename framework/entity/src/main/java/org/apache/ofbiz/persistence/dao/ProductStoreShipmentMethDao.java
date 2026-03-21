package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreShipmentMethEntity;

public interface ProductStoreShipmentMethDao extends CrudDao<ProductStoreShipmentMethEntity, String, SqlBuilder.PSC, ProductStoreShipmentMethDao> , DelegatorQueryDao {
}
