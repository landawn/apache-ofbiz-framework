package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentRevisionEntity;

public interface ContentRevisionDao extends CrudDao<ContentRevisionEntity, ContentRevisionEntity, SqlBuilder.PSC, ContentRevisionDao> {
}
