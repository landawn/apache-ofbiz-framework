package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceContentEntity;

public interface InvoiceContentDao extends CrudDao<InvoiceContentEntity, InvoiceContentEntity, SQLBuilder.PSC, InvoiceContentDao> {
}
