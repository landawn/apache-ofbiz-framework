package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CheckAccountEntity;

public interface CheckAccountDao extends CrudDao<CheckAccountEntity, String, SQLBuilder.PSC, CheckAccountDao> {
}
