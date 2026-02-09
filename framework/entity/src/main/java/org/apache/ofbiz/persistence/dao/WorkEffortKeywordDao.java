package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortKeywordEntity;

public interface WorkEffortKeywordDao extends CrudDao<WorkEffortKeywordEntity, WorkEffortKeywordEntity, SQLBuilder.PSC, WorkEffortKeywordDao> {
}
