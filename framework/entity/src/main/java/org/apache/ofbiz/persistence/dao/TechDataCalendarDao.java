package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TechDataCalendarEntity;

public interface TechDataCalendarDao extends CrudDao<TechDataCalendarEntity, String, SqlBuilder.PSC, TechDataCalendarDao> {
}
