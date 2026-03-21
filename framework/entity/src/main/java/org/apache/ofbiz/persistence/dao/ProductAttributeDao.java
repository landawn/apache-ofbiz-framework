package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductAttributeEntity;

public interface ProductAttributeDao
        extends CrudDao<ProductAttributeEntity, ProductAttributeEntity, SqlBuilder.PSC, ProductAttributeDao>, DelegatorQueryDao {
}
