package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SecurityPermissionEntity;

public interface SecurityPermissionDao extends CrudDao<SecurityPermissionEntity, String, SQLBuilder.PSC, SecurityPermissionDao> {
}
