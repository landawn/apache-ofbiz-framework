package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CatalinaSessionEntity;

public interface CatalinaSessionDao extends CrudDao<CatalinaSessionEntity, String, SQLBuilder.PSC, CatalinaSessionDao> {
}
