package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortPurposeTypeEntity;

public interface WorkEffortPurposeTypeDao extends CrudDao<WorkEffortPurposeTypeEntity, String, SqlBuilder.PSC, WorkEffortPurposeTypeDao> {
}
