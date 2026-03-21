package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTypeEntity;

public interface FinAccountTypeDao extends CrudDao<FinAccountTypeEntity, String, SqlBuilder.PSC, FinAccountTypeDao> {
}
