package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentSearchResultEntity;

public interface ContentSearchResultDao extends CrudDao<ContentSearchResultEntity, String, SqlBuilder.PSC, ContentSearchResultDao> {
}
