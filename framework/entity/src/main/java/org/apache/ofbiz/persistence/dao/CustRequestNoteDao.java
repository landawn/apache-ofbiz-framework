package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestNoteEntity;

public interface CustRequestNoteDao extends CrudDao<CustRequestNoteEntity, CustRequestNoteEntity, SqlBuilder.PSC, CustRequestNoteDao> {
}
