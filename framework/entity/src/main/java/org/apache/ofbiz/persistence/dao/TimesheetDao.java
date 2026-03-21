package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TimesheetEntity;

public interface TimesheetDao extends CrudDao<TimesheetEntity, String, SqlBuilder.PSC, TimesheetDao> {
}
