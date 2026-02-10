package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentEntity;

public interface ContentDao extends CrudDao<ContentEntity, String, SQLBuilder.PSC, ContentDao>, DelegatorQueryDao {
}
