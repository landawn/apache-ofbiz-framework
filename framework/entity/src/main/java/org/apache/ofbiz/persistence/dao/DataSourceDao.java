package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataSourceEntity;

public interface DataSourceDao extends CrudDao<DataSourceEntity, String, SQLBuilder.PSC, DataSourceDao> {
}
