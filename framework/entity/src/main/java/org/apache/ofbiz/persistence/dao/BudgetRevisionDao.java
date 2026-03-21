package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetRevisionEntity;

public interface BudgetRevisionDao extends CrudDao<BudgetRevisionEntity, BudgetRevisionEntity, SqlBuilder.PSC, BudgetRevisionDao> {
}
