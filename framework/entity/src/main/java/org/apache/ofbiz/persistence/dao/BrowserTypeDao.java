package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BrowserTypeEntity;

public interface BrowserTypeDao extends CrudDao<BrowserTypeEntity, String, SqlBuilder.PSC, BrowserTypeDao> {
}
