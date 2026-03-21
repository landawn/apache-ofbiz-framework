package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortCostCalcEntity;

public interface WorkEffortCostCalcDao extends CrudDao<WorkEffortCostCalcEntity, WorkEffortCostCalcEntity, SqlBuilder.PSC, WorkEffortCostCalcDao> , DelegatorQueryDao{
}
