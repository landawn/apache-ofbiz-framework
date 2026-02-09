package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.NoteDataEntity;

public interface NoteDataDao extends CrudDao<NoteDataEntity, String, SQLBuilder.PSC, NoteDataDao> {
}
