package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityLocationGeoPointEntity;

public interface FacilityLocationGeoPointDao extends CrudDao<FacilityLocationGeoPointEntity, FacilityLocationGeoPointEntity, SQLBuilder.PSC, FacilityLocationGeoPointDao> {
}
