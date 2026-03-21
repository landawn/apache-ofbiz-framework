package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataSourceEntity;

public interface DataSourceDao extends CrudDao<DataSourceEntity, String, SqlBuilder.PSC, DataSourceDao> {
}
