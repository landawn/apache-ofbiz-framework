package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortContentTypeEntity;

public interface WorkEffortContentTypeDao extends CrudDao<WorkEffortContentTypeEntity, String, SQLBuilder.PSC, WorkEffortContentTypeDao> {
}
