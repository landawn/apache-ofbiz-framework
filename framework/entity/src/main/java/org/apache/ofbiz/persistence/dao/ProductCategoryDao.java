package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryEntity;

public interface ProductCategoryDao extends CrudDao<ProductCategoryEntity, String, SqlBuilder.PSC, ProductCategoryDao> {
}
