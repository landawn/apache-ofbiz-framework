package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RequirementBudgetAllocationEntity;

public interface RequirementBudgetAllocationDao extends CrudDao<RequirementBudgetAllocationEntity, RequirementBudgetAllocationEntity, SqlBuilder.PSC, RequirementBudgetAllocationDao> {
}
