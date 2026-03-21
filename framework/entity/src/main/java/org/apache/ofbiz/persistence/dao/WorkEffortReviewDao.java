package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortReviewEntity;

public interface WorkEffortReviewDao extends CrudDao<WorkEffortReviewEntity, WorkEffortReviewEntity, SqlBuilder.PSC, WorkEffortReviewDao> {
}
