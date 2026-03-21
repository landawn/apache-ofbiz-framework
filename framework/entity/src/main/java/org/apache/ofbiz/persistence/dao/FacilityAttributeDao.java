package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityAttributeEntity;

public interface FacilityAttributeDao extends CrudDao<FacilityAttributeEntity, FacilityAttributeEntity, SqlBuilder.PSC, FacilityAttributeDao> {
}
