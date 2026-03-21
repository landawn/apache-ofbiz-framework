package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntitySyncEntity;

public interface EntitySyncDao extends CrudDao<EntitySyncEntity, String, SqlBuilder.PSC, EntitySyncDao> {
}
