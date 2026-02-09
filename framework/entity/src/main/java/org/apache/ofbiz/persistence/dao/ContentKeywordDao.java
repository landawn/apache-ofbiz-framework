package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentKeywordEntity;

public interface ContentKeywordDao extends CrudDao<ContentKeywordEntity, ContentKeywordEntity, SQLBuilder.PSC, ContentKeywordDao> {
}
