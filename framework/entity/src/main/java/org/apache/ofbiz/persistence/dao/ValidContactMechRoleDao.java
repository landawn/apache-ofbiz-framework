package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ValidContactMechRoleEntity;

public interface ValidContactMechRoleDao extends CrudDao<ValidContactMechRoleEntity, ValidContactMechRoleEntity, SqlBuilder.PSC, ValidContactMechRoleDao> {
}
