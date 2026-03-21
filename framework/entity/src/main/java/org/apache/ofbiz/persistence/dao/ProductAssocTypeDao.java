package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductAssocTypeEntity;

public interface ProductAssocTypeDao extends CrudDao<ProductAssocTypeEntity, String, SqlBuilder.PSC, ProductAssocTypeDao> {
}
