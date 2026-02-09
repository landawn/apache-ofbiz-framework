package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BrowserTypeEntity;

public interface BrowserTypeDao extends CrudDao<BrowserTypeEntity, String, SQLBuilder.PSC, BrowserTypeDao> {
}
