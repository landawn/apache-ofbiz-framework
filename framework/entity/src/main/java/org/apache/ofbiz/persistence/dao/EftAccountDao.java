package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EftAccountEntity;

public interface EftAccountDao extends CrudDao<EftAccountEntity, String, SQLBuilder.PSC, EftAccountDao> {
}
