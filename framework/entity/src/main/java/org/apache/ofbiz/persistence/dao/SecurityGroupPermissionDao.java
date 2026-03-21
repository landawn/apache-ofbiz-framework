package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SecurityGroupPermissionEntity;

public interface SecurityGroupPermissionDao extends CrudDao<SecurityGroupPermissionEntity, SecurityGroupPermissionEntity, SqlBuilder.PSC, SecurityGroupPermissionDao> {
}
