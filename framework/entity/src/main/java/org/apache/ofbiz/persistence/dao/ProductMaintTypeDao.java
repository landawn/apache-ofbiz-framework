package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductMaintTypeEntity;

public interface ProductMaintTypeDao extends CrudDao<ProductMaintTypeEntity, String, SQLBuilder.PSC, ProductMaintTypeDao> {
}
