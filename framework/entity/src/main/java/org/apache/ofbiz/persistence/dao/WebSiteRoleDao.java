package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebSiteRoleEntity;

public interface WebSiteRoleDao extends CrudDao<WebSiteRoleEntity, WebSiteRoleEntity, SqlBuilder.PSC, WebSiteRoleDao>, DelegatorQueryDao {
}
