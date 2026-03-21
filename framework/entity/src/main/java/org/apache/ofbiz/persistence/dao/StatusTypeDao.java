package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.StatusTypeEntity;

public interface StatusTypeDao extends CrudDao<StatusTypeEntity, String, SqlBuilder.PSC, StatusTypeDao> {
}
