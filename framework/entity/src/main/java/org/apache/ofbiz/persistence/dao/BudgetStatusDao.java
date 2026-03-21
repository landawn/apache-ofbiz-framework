package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetStatusEntity;

public interface BudgetStatusDao extends CrudDao<BudgetStatusEntity, BudgetStatusEntity, SqlBuilder.PSC, BudgetStatusDao> {
}
