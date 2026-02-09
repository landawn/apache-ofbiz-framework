package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupEntity;

public interface FacilityGroupDao extends CrudDao<FacilityGroupEntity, String, SQLBuilder.PSC, FacilityGroupDao> {
}
