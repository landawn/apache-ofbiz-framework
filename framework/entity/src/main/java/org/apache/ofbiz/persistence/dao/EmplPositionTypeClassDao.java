package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplPositionTypeClassEntity;

public interface EmplPositionTypeClassDao extends CrudDao<EmplPositionTypeClassEntity, EmplPositionTypeClassEntity, SQLBuilder.PSC, EmplPositionTypeClassDao> {
}
