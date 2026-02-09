package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceEntity;

public interface InvoiceDao extends CrudDao<InvoiceEntity, String, SQLBuilder.PSC, InvoiceDao> {
}
