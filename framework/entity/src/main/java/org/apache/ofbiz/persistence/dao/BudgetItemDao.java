package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetItemEntity;

public interface BudgetItemDao extends CrudDao<BudgetItemEntity, BudgetItemEntity, SqlBuilder.PSC, BudgetItemDao> {
}
