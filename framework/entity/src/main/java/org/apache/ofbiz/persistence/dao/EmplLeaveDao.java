package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmplLeaveEntity;

public interface EmplLeaveDao extends CrudDao<EmplLeaveEntity, EmplLeaveEntity, SQLBuilder.PSC, EmplLeaveDao> {
}
