package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.StatusItemEntity;

public interface StatusItemDao extends CrudDao<StatusItemEntity, String, SqlBuilder.PSC, StatusItemDao> {
}
