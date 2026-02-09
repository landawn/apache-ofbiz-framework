package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceNoteEntity;

public interface InvoiceNoteDao extends CrudDao<InvoiceNoteEntity, InvoiceNoteEntity, SQLBuilder.PSC, InvoiceNoteDao> {
}
