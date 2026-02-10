package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductEntity;

public interface ProductDao extends CrudDao<ProductEntity, String, SQLBuilder.PSC, ProductDao>, DelegatorQueryDao {
}
