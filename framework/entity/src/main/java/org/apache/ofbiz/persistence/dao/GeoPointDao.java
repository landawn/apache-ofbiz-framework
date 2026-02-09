package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GeoPointEntity;

public interface GeoPointDao extends CrudDao<GeoPointEntity, String, SQLBuilder.PSC, GeoPointDao> {
}
