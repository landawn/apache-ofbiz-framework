package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DocumentTypeAttrEntity;

public interface DocumentTypeAttrDao extends CrudDao<DocumentTypeAttrEntity, DocumentTypeAttrEntity, SqlBuilder.PSC, DocumentTypeAttrDao> {
}
