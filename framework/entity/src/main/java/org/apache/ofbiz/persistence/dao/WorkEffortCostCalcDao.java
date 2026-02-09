package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortCostCalcEntity;

public interface WorkEffortCostCalcDao extends CrudDao<WorkEffortCostCalcEntity, WorkEffortCostCalcEntity, SQLBuilder.PSC, WorkEffortCostCalcDao> {
}
