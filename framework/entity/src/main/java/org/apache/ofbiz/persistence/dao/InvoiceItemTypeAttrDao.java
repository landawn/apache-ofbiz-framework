package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeAttrEntity;

public interface InvoiceItemTypeAttrDao extends CrudDao<InvoiceItemTypeAttrEntity, InvoiceItemTypeAttrEntity, SQLBuilder.PSC, InvoiceItemTypeAttrDao> {
}
