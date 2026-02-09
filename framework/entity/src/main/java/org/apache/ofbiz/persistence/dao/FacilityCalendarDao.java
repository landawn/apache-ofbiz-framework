package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityCalendarEntity;

public interface FacilityCalendarDao extends CrudDao<FacilityCalendarEntity, FacilityCalendarEntity, SQLBuilder.PSC, FacilityCalendarDao> {
}
