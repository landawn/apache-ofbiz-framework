package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemAttributeEntity;

public interface InvoiceItemAttributeDao extends CrudDao<InvoiceItemAttributeEntity, InvoiceItemAttributeEntity, SQLBuilder.PSC, InvoiceItemAttributeDao> {
}
