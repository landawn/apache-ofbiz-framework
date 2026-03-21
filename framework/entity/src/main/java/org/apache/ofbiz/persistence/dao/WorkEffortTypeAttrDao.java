package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortTypeAttrEntity;

public interface WorkEffortTypeAttrDao extends CrudDao<WorkEffortTypeAttrEntity, WorkEffortTypeAttrEntity, SqlBuilder.PSC, WorkEffortTypeAttrDao> {
}
