package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigItemEntity;

public interface ProductConfigItemDao extends CrudDao<ProductConfigItemEntity, String, SqlBuilder.PSC, ProductConfigItemDao> {
}
