package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortTypeEntity;

public interface WorkEffortTypeDao extends CrudDao<WorkEffortTypeEntity, String, SQLBuilder.PSC, WorkEffortTypeDao> {
}
