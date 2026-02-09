package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DocumentTypeEntity;

public interface DocumentTypeDao extends CrudDao<DocumentTypeEntity, String, SQLBuilder.PSC, DocumentTypeDao> {
}
