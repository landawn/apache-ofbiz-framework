package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceTypeEntity;

public interface DataResourceTypeDao extends CrudDao<DataResourceTypeEntity, String, SqlBuilder.PSC, DataResourceTypeDao> {
}
