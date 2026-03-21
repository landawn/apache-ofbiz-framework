package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentPurposeEntity;

public interface ContentPurposeDao extends CrudDao<ContentPurposeEntity, ContentPurposeEntity, SqlBuilder.PSC, ContentPurposeDao> {
}
