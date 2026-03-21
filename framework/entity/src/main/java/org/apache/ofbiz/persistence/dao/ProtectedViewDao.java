package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProtectedViewEntity;

public interface ProtectedViewDao extends CrudDao<ProtectedViewEntity, ProtectedViewEntity, SqlBuilder.PSC, ProtectedViewDao> {
}
