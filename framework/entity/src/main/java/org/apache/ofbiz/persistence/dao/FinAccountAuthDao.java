package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountAuthEntity;

public interface FinAccountAuthDao extends CrudDao<FinAccountAuthEntity, String, SQLBuilder.PSC, FinAccountAuthDao> {
}
