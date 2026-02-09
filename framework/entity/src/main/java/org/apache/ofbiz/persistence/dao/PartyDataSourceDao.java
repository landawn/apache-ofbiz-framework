package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyDataSourceEntity;

public interface PartyDataSourceDao extends CrudDao<PartyDataSourceEntity, PartyDataSourceEntity, SQLBuilder.PSC, PartyDataSourceDao> {
}
