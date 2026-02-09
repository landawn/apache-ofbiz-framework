package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortAttributeEntity;

public interface WorkEffortAttributeDao extends CrudDao<WorkEffortAttributeEntity, WorkEffortAttributeEntity, SQLBuilder.PSC, WorkEffortAttributeDao> {
}
