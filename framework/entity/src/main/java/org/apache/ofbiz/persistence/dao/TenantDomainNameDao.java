package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TenantDomainNameEntity;

public interface TenantDomainNameDao extends CrudDao<TenantDomainNameEntity, String, SqlBuilder.PSC, TenantDomainNameDao> {
}
