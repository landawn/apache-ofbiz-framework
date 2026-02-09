package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionEntity;

public interface EmplPositionDao extends CrudDao<EmplPositionEntity, String, SQLBuilder.PSC, EmplPositionDao> {
}
