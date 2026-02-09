package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GeoAssocEntity;

public interface GeoAssocDao extends CrudDao<GeoAssocEntity, GeoAssocEntity, SQLBuilder.PSC, GeoAssocDao> {
}
