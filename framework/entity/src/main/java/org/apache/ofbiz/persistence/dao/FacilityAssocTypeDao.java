package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityAssocTypeEntity;

public interface FacilityAssocTypeDao extends CrudDao<FacilityAssocTypeEntity, String, SQLBuilder.PSC, FacilityAssocTypeDao> {
}
