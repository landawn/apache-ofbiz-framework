package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GeoTypeEntity;

public interface GeoTypeDao extends CrudDao<GeoTypeEntity, String, SQLBuilder.PSC, GeoTypeDao> {
}
