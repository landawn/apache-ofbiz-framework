package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionTypeEntity;

public interface EmplPositionTypeDao extends CrudDao<EmplPositionTypeEntity, String, SQLBuilder.PSC, EmplPositionTypeDao> {
}
