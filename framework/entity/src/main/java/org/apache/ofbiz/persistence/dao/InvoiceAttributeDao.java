package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceAttributeEntity;

public interface InvoiceAttributeDao extends CrudDao<InvoiceAttributeEntity, InvoiceAttributeEntity, SqlBuilder.PSC, InvoiceAttributeDao> {
}
