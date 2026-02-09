package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryGlAccountEntity;

public interface ProductCategoryGlAccountDao extends CrudDao<ProductCategoryGlAccountEntity, ProductCategoryGlAccountEntity, SQLBuilder.PSC, ProductCategoryGlAccountDao> {
}
