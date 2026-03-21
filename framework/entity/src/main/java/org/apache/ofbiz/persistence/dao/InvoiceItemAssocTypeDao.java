package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemAssocTypeEntity;

public interface InvoiceItemAssocTypeDao extends CrudDao<InvoiceItemAssocTypeEntity, String, SqlBuilder.PSC, InvoiceItemAssocTypeDao> {
}
