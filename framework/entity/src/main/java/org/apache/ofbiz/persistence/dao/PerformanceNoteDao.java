package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PerformanceNoteEntity;

public interface PerformanceNoteDao extends CrudDao<PerformanceNoteEntity, PerformanceNoteEntity, SQLBuilder.PSC, PerformanceNoteDao> {
}
