package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentTypeEntity;

public interface ContentTypeDao extends CrudDao<ContentTypeEntity, String, SQLBuilder.PSC, ContentTypeDao> {
}
