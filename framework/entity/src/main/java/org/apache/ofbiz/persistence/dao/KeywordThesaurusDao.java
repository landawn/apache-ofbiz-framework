package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.KeywordThesaurusEntity;

public interface KeywordThesaurusDao extends CrudDao<KeywordThesaurusEntity, KeywordThesaurusEntity, SqlBuilder.PSC, KeywordThesaurusDao> {
}
