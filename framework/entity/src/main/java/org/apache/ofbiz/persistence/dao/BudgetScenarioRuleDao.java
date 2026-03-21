package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetScenarioRuleEntity;

public interface BudgetScenarioRuleDao extends CrudDao<BudgetScenarioRuleEntity, BudgetScenarioRuleEntity, SqlBuilder.PSC, BudgetScenarioRuleDao> {
}
