package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TechDataCalendarExcDayEntity;

public interface TechDataCalendarExcDayDao extends CrudDao<TechDataCalendarExcDayEntity, TechDataCalendarExcDayEntity, SqlBuilder.PSC, TechDataCalendarExcDayDao>, DelegatorQueryDao {
}
