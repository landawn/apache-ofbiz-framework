package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceEntity;

public interface DataResourceDao extends CrudDao<DataResourceEntity, String, SQLBuilder.PSC, DataResourceDao>, DelegatorQueryDao {
}
