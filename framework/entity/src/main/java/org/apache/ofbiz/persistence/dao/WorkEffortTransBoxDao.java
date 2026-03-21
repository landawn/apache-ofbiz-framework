package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortTransBoxEntity;

public interface WorkEffortTransBoxDao extends CrudDao<WorkEffortTransBoxEntity, WorkEffortTransBoxEntity, SqlBuilder.PSC, WorkEffortTransBoxDao> {
}
