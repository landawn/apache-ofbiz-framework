package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WebPageEntity;

public interface WebPageDao extends CrudDao<WebPageEntity, String, SQLBuilder.PSC, WebPageDao> {
}
