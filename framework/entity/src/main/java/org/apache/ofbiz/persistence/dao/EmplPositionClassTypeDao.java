package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionClassTypeEntity;

public interface EmplPositionClassTypeDao extends CrudDao<EmplPositionClassTypeEntity, String, SqlBuilder.PSC, EmplPositionClassTypeDao> {
}
