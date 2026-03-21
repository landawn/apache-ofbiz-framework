package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetRoleEntity;

public interface BudgetRoleDao extends CrudDao<BudgetRoleEntity, BudgetRoleEntity, SqlBuilder.PSC, BudgetRoleDao> {
}
