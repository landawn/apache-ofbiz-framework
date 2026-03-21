package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SecurityGroupEntity;

public interface SecurityGroupDao extends CrudDao<SecurityGroupEntity, String, SqlBuilder.PSC, SecurityGroupDao> {
}
