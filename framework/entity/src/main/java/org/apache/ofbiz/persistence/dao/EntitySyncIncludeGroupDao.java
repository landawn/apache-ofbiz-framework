package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncIncludeGroupEntity;

public interface EntitySyncIncludeGroupDao extends CrudDao<EntitySyncIncludeGroupEntity, EntitySyncIncludeGroupEntity, SqlBuilder.PSC, EntitySyncIncludeGroupDao> {
}
