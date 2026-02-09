package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PicklistRoleEntity;

public interface PicklistRoleDao extends CrudDao<PicklistRoleEntity, PicklistRoleEntity, SQLBuilder.PSC, PicklistRoleDao> {
}
