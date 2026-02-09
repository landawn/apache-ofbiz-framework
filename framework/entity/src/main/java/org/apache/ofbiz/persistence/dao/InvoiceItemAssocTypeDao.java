package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemAssocTypeEntity;

public interface InvoiceItemAssocTypeDao extends CrudDao<InvoiceItemAssocTypeEntity, String, SQLBuilder.PSC, InvoiceItemAssocTypeDao> {
}
