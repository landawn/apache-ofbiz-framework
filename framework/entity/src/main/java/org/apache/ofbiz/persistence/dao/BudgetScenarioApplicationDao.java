package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetScenarioApplicationEntity;

public interface BudgetScenarioApplicationDao extends CrudDao<BudgetScenarioApplicationEntity, BudgetScenarioApplicationEntity, SQLBuilder.PSC, BudgetScenarioApplicationDao> {
}
