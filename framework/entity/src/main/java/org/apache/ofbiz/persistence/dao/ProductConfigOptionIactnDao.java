package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigOptionIactnEntity;

public interface ProductConfigOptionIactnDao extends CrudDao<ProductConfigOptionIactnEntity, ProductConfigOptionIactnEntity, SqlBuilder.PSC, ProductConfigOptionIactnDao> {
}
