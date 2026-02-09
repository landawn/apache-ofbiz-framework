package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortPurposeTypeEntity;

public interface WorkEffortPurposeTypeDao extends CrudDao<WorkEffortPurposeTypeEntity, String, SQLBuilder.PSC, WorkEffortPurposeTypeDao> {
}
