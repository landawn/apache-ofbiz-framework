package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryRoleEntity;

public interface ProductCategoryRoleDao extends CrudDao<ProductCategoryRoleEntity, ProductCategoryRoleEntity, SqlBuilder.PSC, ProductCategoryRoleDao> {
}
