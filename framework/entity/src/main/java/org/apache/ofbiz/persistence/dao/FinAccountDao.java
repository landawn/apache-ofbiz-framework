package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountEntity;

public interface FinAccountDao extends CrudDao<FinAccountEntity, String, SqlBuilder.PSC, FinAccountDao> {
}
