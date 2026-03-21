package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortInventoryAssignEntity;

public interface WorkEffortInventoryAssignDao extends CrudDao<WorkEffortInventoryAssignEntity, WorkEffortInventoryAssignEntity, SqlBuilder.PSC, WorkEffortInventoryAssignDao> , DelegatorQueryDao{
}
