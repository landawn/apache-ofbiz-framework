package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionTypeEntity;

public interface EmplPositionTypeDao extends CrudDao<EmplPositionTypeEntity, String, SqlBuilder.PSC, EmplPositionTypeDao> {
}
