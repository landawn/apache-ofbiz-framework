package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentRoleEntity;

public interface ContentRoleDao extends CrudDao<ContentRoleEntity, ContentRoleEntity, SqlBuilder.PSC, ContentRoleDao>, DelegatorQueryDao {
}
