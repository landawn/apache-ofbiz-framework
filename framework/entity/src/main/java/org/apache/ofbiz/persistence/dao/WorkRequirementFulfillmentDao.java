package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkRequirementFulfillmentEntity;

public interface WorkRequirementFulfillmentDao extends CrudDao<WorkRequirementFulfillmentEntity, WorkRequirementFulfillmentEntity, SqlBuilder.PSC, WorkRequirementFulfillmentDao> {
}
