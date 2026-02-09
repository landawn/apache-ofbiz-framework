package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EntityGroupEntryEntity;

public interface EntityGroupEntryDao extends CrudDao<EntityGroupEntryEntity, EntityGroupEntryEntity, SQLBuilder.PSC, EntityGroupEntryDao> {
}
