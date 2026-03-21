package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntityKeyStoreEntity;

public interface EntityKeyStoreDao extends CrudDao<EntityKeyStoreEntity, String, SqlBuilder.PSC, EntityKeyStoreDao> {
}
