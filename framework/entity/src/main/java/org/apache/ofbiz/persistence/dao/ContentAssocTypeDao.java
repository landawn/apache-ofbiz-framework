package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentAssocTypeEntity;

public interface ContentAssocTypeDao extends CrudDao<ContentAssocTypeEntity, String, SqlBuilder.PSC, ContentAssocTypeDao> {
}
