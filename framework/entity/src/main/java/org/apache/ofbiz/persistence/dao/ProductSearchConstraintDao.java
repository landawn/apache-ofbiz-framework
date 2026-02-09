package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductSearchConstraintEntity;

public interface ProductSearchConstraintDao extends CrudDao<ProductSearchConstraintEntity, ProductSearchConstraintEntity, SQLBuilder.PSC, ProductSearchConstraintDao> {
}
