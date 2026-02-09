package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SecurityGroupPermissionEntity;

public interface SecurityGroupPermissionDao extends CrudDao<SecurityGroupPermissionEntity, SecurityGroupPermissionEntity, SQLBuilder.PSC, SecurityGroupPermissionDao> {
}
