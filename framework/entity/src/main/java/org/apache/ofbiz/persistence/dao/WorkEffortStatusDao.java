package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortStatusEntity;

public interface WorkEffortStatusDao extends CrudDao<WorkEffortStatusEntity, WorkEffortStatusEntity, SQLBuilder.PSC, WorkEffortStatusDao> {
}
