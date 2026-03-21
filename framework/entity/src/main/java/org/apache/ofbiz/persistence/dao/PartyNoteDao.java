package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyNoteEntity;

public interface PartyNoteDao extends CrudDao<PartyNoteEntity, PartyNoteEntity, SqlBuilder.PSC, PartyNoteDao> {
}
