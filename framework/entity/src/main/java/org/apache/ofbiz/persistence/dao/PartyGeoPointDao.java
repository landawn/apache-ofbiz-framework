package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyGeoPointEntity;

public interface PartyGeoPointDao extends CrudDao<PartyGeoPointEntity, PartyGeoPointEntity, SQLBuilder.PSC, PartyGeoPointDao> {
}
