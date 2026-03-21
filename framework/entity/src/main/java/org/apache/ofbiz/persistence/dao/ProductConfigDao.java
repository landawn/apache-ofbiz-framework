package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigEntity;

public interface ProductConfigDao extends CrudDao<ProductConfigEntity, ProductConfigEntity, SqlBuilder.PSC, ProductConfigDao> {
}
