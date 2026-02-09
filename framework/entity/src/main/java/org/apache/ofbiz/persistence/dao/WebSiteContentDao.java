package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WebSiteContentEntity;

public interface WebSiteContentDao extends CrudDao<WebSiteContentEntity, WebSiteContentEntity, SQLBuilder.PSC, WebSiteContentDao> {
}
