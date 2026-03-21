package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentPurposeTypeEntity;

public interface ContentPurposeTypeDao extends CrudDao<ContentPurposeTypeEntity, String, SqlBuilder.PSC, ContentPurposeTypeDao> {
}
