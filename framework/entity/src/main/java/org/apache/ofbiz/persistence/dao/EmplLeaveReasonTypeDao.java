package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmplLeaveReasonTypeEntity;

public interface EmplLeaveReasonTypeDao extends CrudDao<EmplLeaveReasonTypeEntity, String, SqlBuilder.PSC, EmplLeaveReasonTypeDao> {
}
