package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EntityAuditLogEntity;

public interface EntityAuditLogDao extends CrudDao<EntityAuditLogEntity, String, SQLBuilder.PSC, EntityAuditLogDao> {
}
