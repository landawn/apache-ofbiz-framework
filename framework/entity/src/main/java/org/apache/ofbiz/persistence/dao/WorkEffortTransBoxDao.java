package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortTransBoxEntity;

public interface WorkEffortTransBoxDao extends CrudDao<WorkEffortTransBoxEntity, WorkEffortTransBoxEntity, SQLBuilder.PSC, WorkEffortTransBoxDao> {
}
