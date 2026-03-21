package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentAssocEntity;

public interface ContentAssocDao extends CrudDao<ContentAssocEntity, ContentAssocEntity, SqlBuilder.PSC, ContentAssocDao>, DelegatorQueryDao {
}
