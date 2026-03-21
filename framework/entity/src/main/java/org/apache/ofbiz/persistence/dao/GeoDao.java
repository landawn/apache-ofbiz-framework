package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GeoEntity;

public interface GeoDao extends CrudDao<GeoEntity, String, SqlBuilder.PSC, GeoDao> , DelegatorQueryDao{
}
