package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TimeEntryEntity;

public interface TimeEntryDao extends CrudDao<TimeEntryEntity, String, SQLBuilder.PSC, TimeEntryDao> {
}
