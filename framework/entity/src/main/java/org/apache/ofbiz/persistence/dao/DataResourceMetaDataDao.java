package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceMetaDataEntity;

public interface DataResourceMetaDataDao extends CrudDao<DataResourceMetaDataEntity, DataResourceMetaDataEntity, SqlBuilder.PSC, DataResourceMetaDataDao> {
}
