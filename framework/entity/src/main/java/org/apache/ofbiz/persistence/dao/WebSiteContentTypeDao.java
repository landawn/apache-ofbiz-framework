package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebSiteContentTypeEntity;

public interface WebSiteContentTypeDao extends CrudDao<WebSiteContentTypeEntity, String, SqlBuilder.PSC, WebSiteContentTypeDao> {
}
