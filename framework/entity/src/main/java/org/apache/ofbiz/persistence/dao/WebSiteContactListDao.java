package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebSiteContactListEntity;

public interface WebSiteContactListDao extends CrudDao<WebSiteContactListEntity, WebSiteContactListEntity, SqlBuilder.PSC, WebSiteContactListDao> {
}
