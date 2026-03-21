package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetEntity;

public interface BudgetDao extends CrudDao<BudgetEntity, String, SqlBuilder.PSC, BudgetDao> {
}
