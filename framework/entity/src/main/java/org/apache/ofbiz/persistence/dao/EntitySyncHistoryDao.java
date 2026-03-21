package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncHistoryEntity;

public interface EntitySyncHistoryDao extends CrudDao<EntitySyncHistoryEntity, EntitySyncHistoryEntity, SqlBuilder.PSC, EntitySyncHistoryDao> {
}
