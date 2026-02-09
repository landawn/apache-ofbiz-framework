package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataSourceTypeEntity;

public interface DataSourceTypeDao extends CrudDao<DataSourceTypeEntity, String, SQLBuilder.PSC, DataSourceTypeDao> {
}
