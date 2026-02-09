package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortAssocAttributeEntity;

public interface WorkEffortAssocAttributeDao extends CrudDao<WorkEffortAssocAttributeEntity, WorkEffortAssocAttributeEntity, SQLBuilder.PSC, WorkEffortAssocAttributeDao> {
}
