package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryGlAccountEntity;

public interface ProductCategoryGlAccountDao extends CrudDao<ProductCategoryGlAccountEntity, ProductCategoryGlAccountEntity, SqlBuilder.PSC, ProductCategoryGlAccountDao> {
}
