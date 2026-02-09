package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetAttributeEntity;

public interface BudgetAttributeDao extends CrudDao<BudgetAttributeEntity, BudgetAttributeEntity, SQLBuilder.PSC, BudgetAttributeDao> {
}
