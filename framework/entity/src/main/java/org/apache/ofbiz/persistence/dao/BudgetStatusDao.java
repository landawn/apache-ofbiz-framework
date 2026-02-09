package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetStatusEntity;

public interface BudgetStatusDao extends CrudDao<BudgetStatusEntity, BudgetStatusEntity, SQLBuilder.PSC, BudgetStatusDao> {
}
