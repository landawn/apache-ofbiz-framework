package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountStatusEntity;

public interface FinAccountStatusDao extends CrudDao<FinAccountStatusEntity, FinAccountStatusEntity, SqlBuilder.PSC, FinAccountStatusDao> {
}
