package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebSitePublishPointEntity;

public interface WebSitePublishPointDao extends CrudDao<WebSitePublishPointEntity, String, SqlBuilder.PSC, WebSitePublishPointDao> {
}
