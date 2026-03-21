package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AccommodationClassEntity;

public interface AccommodationClassDao extends CrudDao<AccommodationClassEntity, String, SqlBuilder.PSC, AccommodationClassDao> {
}
