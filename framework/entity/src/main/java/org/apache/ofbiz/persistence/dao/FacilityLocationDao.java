package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityLocationEntity;

public interface FacilityLocationDao extends CrudDao<FacilityLocationEntity, FacilityLocationEntity, SqlBuilder.PSC, FacilityLocationDao> {
}
