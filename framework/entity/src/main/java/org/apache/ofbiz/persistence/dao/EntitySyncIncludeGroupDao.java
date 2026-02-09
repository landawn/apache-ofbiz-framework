package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncIncludeGroupEntity;

public interface EntitySyncIncludeGroupDao extends CrudDao<EntitySyncIncludeGroupEntity, EntitySyncIncludeGroupEntity, SQLBuilder.PSC, EntitySyncIncludeGroupDao> {
}
