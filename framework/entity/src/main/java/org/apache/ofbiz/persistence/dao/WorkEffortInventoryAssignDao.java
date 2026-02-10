package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortInventoryAssignEntity;

public interface WorkEffortInventoryAssignDao extends CrudDao<WorkEffortInventoryAssignEntity, WorkEffortInventoryAssignEntity, SQLBuilder.PSC, WorkEffortInventoryAssignDao> , DelegatorQueryDao{
}
