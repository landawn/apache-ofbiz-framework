package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityTypeEntity;

public interface FacilityTypeDao extends CrudDao<FacilityTypeEntity, String, SqlBuilder.PSC, FacilityTypeDao> {
}
