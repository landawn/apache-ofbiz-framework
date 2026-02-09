package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteNoteEntity;

public interface QuoteNoteDao extends CrudDao<QuoteNoteEntity, QuoteNoteEntity, SQLBuilder.PSC, QuoteNoteDao> {
}
