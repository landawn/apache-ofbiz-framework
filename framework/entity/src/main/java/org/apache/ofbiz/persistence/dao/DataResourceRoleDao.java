package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceRoleEntity;

public interface DataResourceRoleDao extends CrudDao<DataResourceRoleEntity, DataResourceRoleEntity, SqlBuilder.PSC, DataResourceRoleDao> {
}
