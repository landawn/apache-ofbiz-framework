package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityTypeAttrEntity;

public interface FacilityTypeAttrDao extends CrudDao<FacilityTypeAttrEntity, FacilityTypeAttrEntity, SqlBuilder.PSC, FacilityTypeAttrDao> {
}
