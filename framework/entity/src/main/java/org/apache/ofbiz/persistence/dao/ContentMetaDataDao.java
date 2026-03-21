package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentMetaDataEntity;

public interface ContentMetaDataDao extends CrudDao<ContentMetaDataEntity, ContentMetaDataEntity, SqlBuilder.PSC, ContentMetaDataDao> {
}
