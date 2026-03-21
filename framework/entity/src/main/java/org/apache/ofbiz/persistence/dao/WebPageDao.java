package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebPageEntity;

public interface WebPageDao extends CrudDao<WebPageEntity, String, SqlBuilder.PSC, WebPageDao> {
}
