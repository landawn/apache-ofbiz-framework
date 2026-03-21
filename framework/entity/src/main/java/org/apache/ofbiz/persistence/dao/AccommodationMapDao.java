package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AccommodationMapEntity;

public interface AccommodationMapDao extends CrudDao<AccommodationMapEntity, String, SqlBuilder.PSC, AccommodationMapDao> {
}
