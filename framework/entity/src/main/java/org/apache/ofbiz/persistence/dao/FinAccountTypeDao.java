package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTypeEntity;

public interface FinAccountTypeDao extends CrudDao<FinAccountTypeEntity, String, SQLBuilder.PSC, FinAccountTypeDao> {
}
