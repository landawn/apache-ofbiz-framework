package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemEntity;

public interface InvoiceItemDao extends CrudDao<InvoiceItemEntity, InvoiceItemEntity, SQLBuilder.PSC, InvoiceItemDao> {
}
