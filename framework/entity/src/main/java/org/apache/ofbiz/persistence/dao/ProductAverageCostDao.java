package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductAverageCostEntity;

public interface ProductAverageCostDao extends CrudDao<ProductAverageCostEntity, ProductAverageCostEntity, SqlBuilder.PSC, ProductAverageCostDao> {
}
