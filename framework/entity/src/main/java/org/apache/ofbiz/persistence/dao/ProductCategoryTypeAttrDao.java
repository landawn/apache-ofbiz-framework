package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryTypeAttrEntity;

public interface ProductCategoryTypeAttrDao extends CrudDao<ProductCategoryTypeAttrEntity, ProductCategoryTypeAttrEntity, SQLBuilder.PSC, ProductCategoryTypeAttrDao> {
}
