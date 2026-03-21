package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkEffortNoteEntity;

public interface WorkEffortNoteDao extends CrudDao<WorkEffortNoteEntity, WorkEffortNoteEntity, SqlBuilder.PSC, WorkEffortNoteDao> {
}
