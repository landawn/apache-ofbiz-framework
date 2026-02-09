package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyNoteEntity;

public interface PartyNoteDao extends CrudDao<PartyNoteEntity, PartyNoteEntity, SQLBuilder.PSC, PartyNoteDao> {
}
