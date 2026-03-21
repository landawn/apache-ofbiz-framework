package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebAnalyticsConfigEntity;

public interface WebAnalyticsConfigDao extends CrudDao<WebAnalyticsConfigEntity, WebAnalyticsConfigEntity, SqlBuilder.PSC, WebAnalyticsConfigDao> {
}
