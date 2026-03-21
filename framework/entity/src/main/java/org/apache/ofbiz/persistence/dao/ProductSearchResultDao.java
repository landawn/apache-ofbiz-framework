package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductSearchResultEntity;

public interface ProductSearchResultDao extends CrudDao<ProductSearchResultEntity, String, SqlBuilder.PSC, ProductSearchResultDao> {
}
