package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductTypeEntity;

public interface ProductTypeDao extends CrudDao<ProductTypeEntity, String, SQLBuilder.PSC, ProductTypeDao> {
}
