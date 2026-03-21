package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebSiteContentEntity;

public interface WebSiteContentDao extends CrudDao<WebSiteContentEntity, WebSiteContentEntity, SqlBuilder.PSC, WebSiteContentDao> {
}
