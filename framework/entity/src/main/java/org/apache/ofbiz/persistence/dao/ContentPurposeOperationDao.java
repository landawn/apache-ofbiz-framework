package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentPurposeOperationEntity;

public interface ContentPurposeOperationDao extends CrudDao<ContentPurposeOperationEntity, ContentPurposeOperationEntity, SqlBuilder.PSC, ContentPurposeOperationDao> {
}
