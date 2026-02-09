package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplLeaveTypeEntity;

public interface EmplLeaveTypeDao extends CrudDao<EmplLeaveTypeEntity, String, SQLBuilder.PSC, EmplLeaveTypeDao> {
}
