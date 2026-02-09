package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionClassTypeEntity;

public interface EmplPositionClassTypeDao extends CrudDao<EmplPositionClassTypeEntity, String, SQLBuilder.PSC, EmplPositionClassTypeDao> {
}
