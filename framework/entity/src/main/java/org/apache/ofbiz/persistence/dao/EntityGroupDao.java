package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EntityGroupEntity;

public interface EntityGroupDao extends CrudDao<EntityGroupEntity, String, SQLBuilder.PSC, EntityGroupDao> {
}
