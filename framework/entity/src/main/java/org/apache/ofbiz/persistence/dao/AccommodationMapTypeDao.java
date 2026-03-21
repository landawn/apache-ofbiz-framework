package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AccommodationMapTypeEntity;

public interface AccommodationMapTypeDao extends CrudDao<AccommodationMapTypeEntity, String, SqlBuilder.PSC, AccommodationMapTypeDao> {
}
