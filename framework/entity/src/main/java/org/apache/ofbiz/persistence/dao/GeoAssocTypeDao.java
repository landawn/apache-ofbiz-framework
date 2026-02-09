package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GeoAssocTypeEntity;

public interface GeoAssocTypeDao extends CrudDao<GeoAssocTypeEntity, String, SQLBuilder.PSC, GeoAssocTypeDao> {
}
