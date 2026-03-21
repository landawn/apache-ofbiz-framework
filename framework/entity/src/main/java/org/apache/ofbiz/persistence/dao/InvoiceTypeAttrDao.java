package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceTypeAttrEntity;

public interface InvoiceTypeAttrDao extends CrudDao<InvoiceTypeAttrEntity, InvoiceTypeAttrEntity, SqlBuilder.PSC, InvoiceTypeAttrDao> {
}
