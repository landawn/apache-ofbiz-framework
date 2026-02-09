package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortEntity;

public interface WorkEffortDao extends CrudDao<WorkEffortEntity, String, SQLBuilder.PSC, WorkEffortDao> {
}
