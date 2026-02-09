package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TenantComponentEntity;

public interface TenantComponentDao extends CrudDao<TenantComponentEntity, TenantComponentEntity, SQLBuilder.PSC, TenantComponentDao> {
}
