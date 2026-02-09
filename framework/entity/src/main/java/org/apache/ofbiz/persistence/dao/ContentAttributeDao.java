package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentAttributeEntity;

public interface ContentAttributeDao extends CrudDao<ContentAttributeEntity, ContentAttributeEntity, SQLBuilder.PSC, ContentAttributeDao> {
}
