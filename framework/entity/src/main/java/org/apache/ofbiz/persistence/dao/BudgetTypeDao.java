package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetTypeEntity;

public interface BudgetTypeDao extends CrudDao<BudgetTypeEntity, String, SqlBuilder.PSC, BudgetTypeDao> {
}
