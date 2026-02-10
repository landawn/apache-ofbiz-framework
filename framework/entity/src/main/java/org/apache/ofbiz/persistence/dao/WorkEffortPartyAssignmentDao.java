package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortPartyAssignmentEntity;

public interface WorkEffortPartyAssignmentDao extends CrudDao<WorkEffortPartyAssignmentEntity, WorkEffortPartyAssignmentEntity, SQLBuilder.PSC, WorkEffortPartyAssignmentDao> , DelegatorQueryDao{
}
