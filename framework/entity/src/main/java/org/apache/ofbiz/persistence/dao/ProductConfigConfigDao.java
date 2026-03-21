package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigConfigEntity;

public interface ProductConfigConfigDao extends CrudDao<ProductConfigConfigEntity, ProductConfigConfigEntity, SqlBuilder.PSC, ProductConfigConfigDao> {
}
