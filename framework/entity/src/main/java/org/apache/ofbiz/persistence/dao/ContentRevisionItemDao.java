package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentRevisionItemEntity;

public interface ContentRevisionItemDao extends CrudDao<ContentRevisionItemEntity, ContentRevisionItemEntity, SQLBuilder.PSC, ContentRevisionItemDao> {
}
