package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupTypeEntity;

public interface FacilityGroupTypeDao extends CrudDao<FacilityGroupTypeEntity, String, SqlBuilder.PSC, FacilityGroupTypeDao> {
}
