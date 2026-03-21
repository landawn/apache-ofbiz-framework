package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AccommodationSpotEntity;

public interface AccommodationSpotDao extends CrudDao<AccommodationSpotEntity, String, SqlBuilder.PSC, AccommodationSpotDao> {
}
