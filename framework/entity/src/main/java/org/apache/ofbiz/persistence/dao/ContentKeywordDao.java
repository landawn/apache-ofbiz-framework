package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentKeywordEntity;

public interface ContentKeywordDao extends CrudDao<ContentKeywordEntity, ContentKeywordEntity, SqlBuilder.PSC, ContentKeywordDao> {
}
