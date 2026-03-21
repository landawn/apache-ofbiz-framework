package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncIncludeEntity;

public interface EntitySyncIncludeDao extends CrudDao<EntitySyncIncludeEntity, EntitySyncIncludeEntity, SqlBuilder.PSC, EntitySyncIncludeDao> {
}
