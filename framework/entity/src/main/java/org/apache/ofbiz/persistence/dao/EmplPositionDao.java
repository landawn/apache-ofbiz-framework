package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionEntity;

public interface EmplPositionDao extends CrudDao<EmplPositionEntity, String, SqlBuilder.PSC, EmplPositionDao> {
}
