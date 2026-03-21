package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortGoodStandardEntity;

public interface WorkEffortGoodStandardDao extends CrudDao<WorkEffortGoodStandardEntity, WorkEffortGoodStandardEntity, SqlBuilder.PSC, WorkEffortGoodStandardDao> , DelegatorQueryDao{
}
