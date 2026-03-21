package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductAverageCostTypeEntity;

public interface ProductAverageCostTypeDao extends CrudDao<ProductAverageCostTypeEntity, String, SqlBuilder.PSC, ProductAverageCostTypeDao> {
}
