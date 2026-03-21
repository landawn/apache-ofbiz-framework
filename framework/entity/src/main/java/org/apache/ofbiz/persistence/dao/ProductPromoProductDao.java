package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoProductEntity;

public interface ProductPromoProductDao extends CrudDao<ProductPromoProductEntity, ProductPromoProductEntity, SqlBuilder.PSC, ProductPromoProductDao> {
}
