package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigProductEntity;

public interface ProductConfigProductDao extends CrudDao<ProductConfigProductEntity, ProductConfigProductEntity, SqlBuilder.PSC, ProductConfigProductDao> {
}
