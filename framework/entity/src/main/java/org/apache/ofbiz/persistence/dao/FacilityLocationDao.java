package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityLocationEntity;

public interface FacilityLocationDao extends CrudDao<FacilityLocationEntity, FacilityLocationEntity, SQLBuilder.PSC, FacilityLocationDao> {
}
