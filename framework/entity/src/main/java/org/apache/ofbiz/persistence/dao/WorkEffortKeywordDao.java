package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortKeywordEntity;

public interface WorkEffortKeywordDao extends CrudDao<WorkEffortKeywordEntity, WorkEffortKeywordEntity, SqlBuilder.PSC, WorkEffortKeywordDao> {
}
