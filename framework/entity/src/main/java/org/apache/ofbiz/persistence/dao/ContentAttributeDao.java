package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentAttributeEntity;

public interface ContentAttributeDao extends CrudDao<ContentAttributeEntity, ContentAttributeEntity, SqlBuilder.PSC, ContentAttributeDao> {
}
