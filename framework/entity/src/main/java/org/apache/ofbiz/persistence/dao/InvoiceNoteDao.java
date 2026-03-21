package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceNoteEntity;

public interface InvoiceNoteDao extends CrudDao<InvoiceNoteEntity, InvoiceNoteEntity, SqlBuilder.PSC, InvoiceNoteDao> {
}
