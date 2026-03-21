package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncRemoveEntity;

public interface EntitySyncRemoveDao extends CrudDao<EntitySyncRemoveEntity, String, SqlBuilder.PSC, EntitySyncRemoveDao> {
}
