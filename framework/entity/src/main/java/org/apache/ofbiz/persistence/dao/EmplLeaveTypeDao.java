package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmplLeaveTypeEntity;

public interface EmplLeaveTypeDao extends CrudDao<EmplLeaveTypeEntity, String, SqlBuilder.PSC, EmplLeaveTypeDao> {
}
