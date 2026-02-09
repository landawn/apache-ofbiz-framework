package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TenantDataSourceEntity;

public interface TenantDataSourceDao extends CrudDao<TenantDataSourceEntity, TenantDataSourceEntity, SQLBuilder.PSC, TenantDataSourceDao> {
}
