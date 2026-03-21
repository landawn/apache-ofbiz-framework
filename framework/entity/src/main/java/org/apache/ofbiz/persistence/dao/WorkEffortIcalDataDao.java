package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortIcalDataEntity;

public interface WorkEffortIcalDataDao extends CrudDao<WorkEffortIcalDataEntity, String, SqlBuilder.PSC, WorkEffortIcalDataDao> {
}
