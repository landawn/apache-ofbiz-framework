package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityContactMechEntity;

public interface FacilityContactMechDao extends CrudDao<FacilityContactMechEntity, FacilityContactMechEntity, SQLBuilder.PSC, FacilityContactMechDao> {
}
