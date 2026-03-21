package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TenantEntity;

public interface TenantDao extends CrudDao<TenantEntity, String, SqlBuilder.PSC, TenantDao> {
}
