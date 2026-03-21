package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortContentEntity;

public interface WorkEffortContentDao extends CrudDao<WorkEffortContentEntity, WorkEffortContentEntity, SqlBuilder.PSC, WorkEffortContentDao> {
}
