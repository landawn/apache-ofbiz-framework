package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityLocationGeoPointEntity;

public interface FacilityLocationGeoPointDao extends CrudDao<FacilityLocationGeoPointEntity, FacilityLocationGeoPointEntity, SqlBuilder.PSC, FacilityLocationGeoPointDao> {
}
