package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetAttributeEntity;

public interface BudgetAttributeDao extends CrudDao<BudgetAttributeEntity, BudgetAttributeEntity, SqlBuilder.PSC, BudgetAttributeDao> {
}
