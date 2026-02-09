package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigConfigEntity;

public interface ProductConfigConfigDao extends CrudDao<ProductConfigConfigEntity, ProductConfigConfigEntity, SQLBuilder.PSC, ProductConfigConfigDao> {
}
