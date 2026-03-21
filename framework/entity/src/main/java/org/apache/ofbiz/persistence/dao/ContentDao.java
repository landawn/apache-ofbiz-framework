package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentEntity;

public interface ContentDao extends CrudDao<ContentEntity, String, SqlBuilder.PSC, ContentDao>, DelegatorQueryDao {
}
