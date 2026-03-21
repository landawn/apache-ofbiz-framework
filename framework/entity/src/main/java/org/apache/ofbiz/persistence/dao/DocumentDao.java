package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DocumentEntity;

public interface DocumentDao extends CrudDao<DocumentEntity, String, SqlBuilder.PSC, DocumentDao> {
}
