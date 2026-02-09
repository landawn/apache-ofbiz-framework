package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetTypeAttrEntity;

public interface BudgetTypeAttrDao extends CrudDao<BudgetTypeAttrEntity, BudgetTypeAttrEntity, SQLBuilder.PSC, BudgetTypeAttrDao> {
}
