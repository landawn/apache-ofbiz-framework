package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceStatusEntity;

public interface InvoiceStatusDao extends CrudDao<InvoiceStatusEntity, InvoiceStatusEntity, SQLBuilder.PSC, InvoiceStatusDao> {
}
