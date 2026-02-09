package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TechDataCalendarWeekEntity;

public interface TechDataCalendarWeekDao extends CrudDao<TechDataCalendarWeekEntity, String, SQLBuilder.PSC, TechDataCalendarWeekDao> {
}
