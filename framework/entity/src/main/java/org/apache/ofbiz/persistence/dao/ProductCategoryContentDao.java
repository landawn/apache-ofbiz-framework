package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryContentEntity;

public interface ProductCategoryContentDao extends CrudDao<ProductCategoryContentEntity, ProductCategoryContentEntity, SqlBuilder.PSC, ProductCategoryContentDao> {
}
