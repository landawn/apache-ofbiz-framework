package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductGlAccountEntity;

public interface ProductGlAccountDao extends CrudDao<ProductGlAccountEntity, ProductGlAccountEntity, SQLBuilder.PSC, ProductGlAccountDao> {
}
