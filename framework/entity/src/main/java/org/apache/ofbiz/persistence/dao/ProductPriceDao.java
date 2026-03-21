package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceEntity;

public interface ProductPriceDao extends CrudDao<ProductPriceEntity, ProductPriceEntity, SqlBuilder.PSC, ProductPriceDao>,
        DelegatorQueryDao {
}
