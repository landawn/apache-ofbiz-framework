package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceAttributeEntity;

public interface InvoiceAttributeDao extends CrudDao<InvoiceAttributeEntity, InvoiceAttributeEntity, SQLBuilder.PSC, InvoiceAttributeDao> {
}
