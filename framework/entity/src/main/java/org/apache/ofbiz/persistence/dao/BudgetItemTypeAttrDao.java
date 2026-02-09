package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetItemTypeAttrEntity;

public interface BudgetItemTypeAttrDao extends CrudDao<BudgetItemTypeAttrEntity, BudgetItemTypeAttrEntity, SQLBuilder.PSC, BudgetItemTypeAttrDao> {
}
