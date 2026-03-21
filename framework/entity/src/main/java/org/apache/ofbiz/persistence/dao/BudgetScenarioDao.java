package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetScenarioEntity;

public interface BudgetScenarioDao extends CrudDao<BudgetScenarioEntity, String, SqlBuilder.PSC, BudgetScenarioDao> {
}
