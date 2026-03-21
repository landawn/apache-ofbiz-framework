package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductContentTypeEntity;

public interface ProductContentTypeDao extends CrudDao<ProductContentTypeEntity, String, SqlBuilder.PSC, ProductContentTypeDao>,
        DelegatorQueryDao {
}
