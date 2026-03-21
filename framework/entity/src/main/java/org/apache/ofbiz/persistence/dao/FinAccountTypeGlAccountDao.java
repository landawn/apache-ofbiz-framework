package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTypeGlAccountEntity;

public interface FinAccountTypeGlAccountDao extends CrudDao<FinAccountTypeGlAccountEntity, FinAccountTypeGlAccountEntity, SqlBuilder.PSC, FinAccountTypeGlAccountDao> {
}
