package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryContentTypeEntity;

public interface ProductCategoryContentTypeDao extends CrudDao<ProductCategoryContentTypeEntity, String, SqlBuilder.PSC, ProductCategoryContentTypeDao> {
}
