package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyGeoPointEntity;

public interface PartyGeoPointDao extends CrudDao<PartyGeoPointEntity, PartyGeoPointEntity, SqlBuilder.PSC, PartyGeoPointDao> {
}
