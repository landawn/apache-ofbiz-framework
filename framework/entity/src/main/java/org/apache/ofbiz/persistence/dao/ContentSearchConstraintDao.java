package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentSearchConstraintEntity;

public interface ContentSearchConstraintDao extends CrudDao<ContentSearchConstraintEntity, ContentSearchConstraintEntity, SQLBuilder.PSC, ContentSearchConstraintDao> {
}
