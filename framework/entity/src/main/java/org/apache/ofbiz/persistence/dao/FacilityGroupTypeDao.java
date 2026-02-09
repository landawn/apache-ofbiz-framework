package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupTypeEntity;

public interface FacilityGroupTypeDao extends CrudDao<FacilityGroupTypeEntity, String, SQLBuilder.PSC, FacilityGroupTypeDao> {
}
