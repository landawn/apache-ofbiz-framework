package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryTypeEntity;

public interface ProductCategoryTypeDao extends CrudDao<ProductCategoryTypeEntity, String, SQLBuilder.PSC, ProductCategoryTypeDao> {
}
