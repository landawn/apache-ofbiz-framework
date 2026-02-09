package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RoleTypeEntity;

public interface RoleTypeDao extends CrudDao<RoleTypeEntity, String, SQLBuilder.PSC, RoleTypeDao> {
}
