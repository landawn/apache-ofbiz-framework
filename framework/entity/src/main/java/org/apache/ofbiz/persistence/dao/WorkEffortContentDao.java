package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortContentEntity;

public interface WorkEffortContentDao extends CrudDao<WorkEffortContentEntity, WorkEffortContentEntity, SQLBuilder.PSC, WorkEffortContentDao> {
}
