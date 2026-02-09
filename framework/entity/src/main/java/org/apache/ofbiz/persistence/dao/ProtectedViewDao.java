package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProtectedViewEntity;

public interface ProtectedViewDao extends CrudDao<ProtectedViewEntity, ProtectedViewEntity, SQLBuilder.PSC, ProtectedViewDao> {
}
