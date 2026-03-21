package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceEntity;

public interface DataResourceDao extends CrudDao<DataResourceEntity, String, SqlBuilder.PSC, DataResourceDao>, DelegatorQueryDao {
}
