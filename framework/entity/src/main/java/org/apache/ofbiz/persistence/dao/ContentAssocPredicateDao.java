package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentAssocPredicateEntity;

public interface ContentAssocPredicateDao extends CrudDao<ContentAssocPredicateEntity, String, SqlBuilder.PSC, ContentAssocPredicateDao> {
}
