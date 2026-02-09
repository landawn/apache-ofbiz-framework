package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductAverageCostEntity;

public interface ProductAverageCostDao extends CrudDao<ProductAverageCostEntity, ProductAverageCostEntity, SQLBuilder.PSC, ProductAverageCostDao> {
}
