package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TenantKeyEncryptingKeyEntity;

public interface TenantKeyEncryptingKeyDao extends CrudDao<TenantKeyEncryptingKeyEntity, String, SqlBuilder.PSC, TenantKeyEncryptingKeyDao> {
}
