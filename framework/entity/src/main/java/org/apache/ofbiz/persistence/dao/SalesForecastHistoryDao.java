package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SalesForecastHistoryEntity;

public interface SalesForecastHistoryDao extends CrudDao<SalesForecastHistoryEntity, String, SQLBuilder.PSC, SalesForecastHistoryDao> {
}
