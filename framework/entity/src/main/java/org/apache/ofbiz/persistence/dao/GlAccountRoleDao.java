package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountRoleEntity;

public interface GlAccountRoleDao extends CrudDao<GlAccountRoleEntity, GlAccountRoleEntity, SQLBuilder.PSC, GlAccountRoleDao> {
}
