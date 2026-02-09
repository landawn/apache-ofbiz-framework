package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestItemNoteEntity;

public interface CustRequestItemNoteDao extends CrudDao<CustRequestItemNoteEntity, CustRequestItemNoteEntity, SQLBuilder.PSC, CustRequestItemNoteDao> {
}
