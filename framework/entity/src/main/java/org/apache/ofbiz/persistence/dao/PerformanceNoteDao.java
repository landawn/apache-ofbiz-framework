package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PerformanceNoteEntity;

public interface PerformanceNoteDao extends CrudDao<PerformanceNoteEntity, PerformanceNoteEntity, SqlBuilder.PSC, PerformanceNoteDao> {
}
