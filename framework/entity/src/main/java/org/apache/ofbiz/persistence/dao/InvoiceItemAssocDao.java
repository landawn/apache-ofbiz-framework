package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemAssocEntity;

public interface InvoiceItemAssocDao extends CrudDao<InvoiceItemAssocEntity, InvoiceItemAssocEntity, SqlBuilder.PSC, InvoiceItemAssocDao> {
}
