package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceMetaDataEntity;

public interface DataResourceMetaDataDao extends CrudDao<DataResourceMetaDataEntity, DataResourceMetaDataEntity, SQLBuilder.PSC, DataResourceMetaDataDao> {
}
