package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RequirementRoleEntity;

public interface RequirementRoleDao extends CrudDao<RequirementRoleEntity, RequirementRoleEntity, SqlBuilder.PSC, RequirementRoleDao> {
}
