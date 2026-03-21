package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductTypeEntity;

public interface ProductTypeDao extends CrudDao<ProductTypeEntity, String, SqlBuilder.PSC, ProductTypeDao> {
}
