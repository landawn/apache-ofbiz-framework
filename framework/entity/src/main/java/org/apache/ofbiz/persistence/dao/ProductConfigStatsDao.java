package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductConfigStatsEntity;

public interface ProductConfigStatsDao extends CrudDao<ProductConfigStatsEntity, ProductConfigStatsEntity, SqlBuilder.PSC, ProductConfigStatsDao> {
}
