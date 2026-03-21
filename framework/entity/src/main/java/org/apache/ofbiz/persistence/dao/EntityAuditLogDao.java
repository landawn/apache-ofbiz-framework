package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EntityAuditLogEntity;

public interface EntityAuditLogDao extends CrudDao<EntityAuditLogEntity, String, SqlBuilder.PSC, EntityAuditLogDao> {
}
