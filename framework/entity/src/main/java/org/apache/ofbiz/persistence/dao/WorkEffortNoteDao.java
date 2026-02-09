package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortNoteEntity;

public interface WorkEffortNoteDao extends CrudDao<WorkEffortNoteEntity, WorkEffortNoteEntity, SQLBuilder.PSC, WorkEffortNoteDao> {
}
