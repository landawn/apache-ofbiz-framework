package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TenantComponentEntity;

public interface TenantComponentDao extends CrudDao<TenantComponentEntity, TenantComponentEntity, SqlBuilder.PSC, TenantComponentDao> {
}
