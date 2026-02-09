package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetItemTypeEntity;

public interface BudgetItemTypeDao extends CrudDao<BudgetItemTypeEntity, String, SQLBuilder.PSC, BudgetItemTypeDao> {
}
