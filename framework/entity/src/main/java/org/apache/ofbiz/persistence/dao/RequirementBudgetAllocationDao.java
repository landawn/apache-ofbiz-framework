package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementBudgetAllocationEntity;

public interface RequirementBudgetAllocationDao extends CrudDao<RequirementBudgetAllocationEntity, RequirementBudgetAllocationEntity, SQLBuilder.PSC, RequirementBudgetAllocationDao> {
}
