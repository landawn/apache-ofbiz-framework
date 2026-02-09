package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ValidContactMechRoleEntity;

public interface ValidContactMechRoleDao extends CrudDao<ValidContactMechRoleEntity, ValidContactMechRoleEntity, SQLBuilder.PSC, ValidContactMechRoleDao> {
}
