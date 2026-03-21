package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceStatusEntity;

public interface InvoiceStatusDao extends CrudDao<InvoiceStatusEntity, InvoiceStatusEntity, SqlBuilder.PSC, InvoiceStatusDao> {
}
