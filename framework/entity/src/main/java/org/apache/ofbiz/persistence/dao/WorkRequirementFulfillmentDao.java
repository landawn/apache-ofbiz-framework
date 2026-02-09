package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkRequirementFulfillmentEntity;

public interface WorkRequirementFulfillmentDao extends CrudDao<WorkRequirementFulfillmentEntity, WorkRequirementFulfillmentEntity, SQLBuilder.PSC, WorkRequirementFulfillmentDao> {
}
