package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityTypeAttrEntity;

public interface FacilityTypeAttrDao extends CrudDao<FacilityTypeAttrEntity, FacilityTypeAttrEntity, SQLBuilder.PSC, FacilityTypeAttrDao> {
}
