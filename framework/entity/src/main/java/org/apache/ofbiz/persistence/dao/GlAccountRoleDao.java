package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountRoleEntity;

public interface GlAccountRoleDao extends CrudDao<GlAccountRoleEntity, GlAccountRoleEntity, SqlBuilder.PSC, GlAccountRoleDao> {
}
