package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortBillingEntity;

public interface WorkEffortBillingDao extends CrudDao<WorkEffortBillingEntity, WorkEffortBillingEntity, SqlBuilder.PSC, WorkEffortBillingDao> {
}
