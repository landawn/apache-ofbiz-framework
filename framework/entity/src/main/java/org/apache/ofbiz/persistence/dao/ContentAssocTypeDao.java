package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentAssocTypeEntity;

public interface ContentAssocTypeDao extends CrudDao<ContentAssocTypeEntity, String, SQLBuilder.PSC, ContentAssocTypeDao> {
}
