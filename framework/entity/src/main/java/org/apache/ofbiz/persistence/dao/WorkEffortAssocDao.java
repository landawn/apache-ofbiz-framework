package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortAssocEntity;

public interface WorkEffortAssocDao extends CrudDao<WorkEffortAssocEntity, WorkEffortAssocEntity, SqlBuilder.PSC, WorkEffortAssocDao> , DelegatorQueryDao{
}
