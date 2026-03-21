package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SalesForecastEntity;

public interface SalesForecastDao extends CrudDao<SalesForecastEntity, String, SqlBuilder.PSC, SalesForecastDao> {
}
