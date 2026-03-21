package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryLinkEntity;

public interface ProductCategoryLinkDao extends CrudDao<ProductCategoryLinkEntity, ProductCategoryLinkEntity, SqlBuilder.PSC, ProductCategoryLinkDao> {
}
