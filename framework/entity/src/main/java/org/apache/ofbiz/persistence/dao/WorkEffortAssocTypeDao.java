package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortAssocTypeEntity;

public interface WorkEffortAssocTypeDao extends CrudDao<WorkEffortAssocTypeEntity, String, SqlBuilder.PSC, WorkEffortAssocTypeDao> {
}
