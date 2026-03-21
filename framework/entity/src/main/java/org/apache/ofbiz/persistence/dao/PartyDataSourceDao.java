package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyDataSourceEntity;

public interface PartyDataSourceDao extends CrudDao<PartyDataSourceEntity, PartyDataSourceEntity, SqlBuilder.PSC, PartyDataSourceDao> {
}
