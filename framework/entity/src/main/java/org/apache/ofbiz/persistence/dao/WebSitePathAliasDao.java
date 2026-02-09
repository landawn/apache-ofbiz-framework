package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WebSitePathAliasEntity;

public interface WebSitePathAliasDao extends CrudDao<WebSitePathAliasEntity, WebSitePathAliasEntity, SQLBuilder.PSC, WebSitePathAliasDao> {
}
