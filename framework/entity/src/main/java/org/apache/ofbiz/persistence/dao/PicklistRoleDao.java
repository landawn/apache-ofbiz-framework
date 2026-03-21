package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PicklistRoleEntity;

public interface PicklistRoleDao extends CrudDao<PicklistRoleEntity, PicklistRoleEntity, SqlBuilder.PSC, PicklistRoleDao> {
}
