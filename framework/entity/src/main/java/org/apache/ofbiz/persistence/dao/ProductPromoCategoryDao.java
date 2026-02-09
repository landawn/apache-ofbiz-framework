package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCategoryEntity;

public interface ProductPromoCategoryDao extends CrudDao<ProductPromoCategoryEntity, ProductPromoCategoryEntity, SQLBuilder.PSC, ProductPromoCategoryDao> {
}
