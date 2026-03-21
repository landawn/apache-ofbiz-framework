package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortAssocAttributeEntity;

public interface WorkEffortAssocAttributeDao extends CrudDao<WorkEffortAssocAttributeEntity, WorkEffortAssocAttributeEntity, SqlBuilder.PSC, WorkEffortAssocAttributeDao> {
}
