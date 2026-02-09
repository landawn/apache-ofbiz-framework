package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DocumentAttributeEntity;

public interface DocumentAttributeDao extends CrudDao<DocumentAttributeEntity, DocumentAttributeEntity, SQLBuilder.PSC, DocumentAttributeDao> {
}
