package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestItemNoteEntity;

public interface CustRequestItemNoteDao extends CrudDao<CustRequestItemNoteEntity, CustRequestItemNoteEntity, SqlBuilder.PSC, CustRequestItemNoteDao> {
}
