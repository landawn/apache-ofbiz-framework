package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeAttrEntity;

public interface InvoiceItemTypeAttrDao extends CrudDao<InvoiceItemTypeAttrEntity, InvoiceItemTypeAttrEntity, SqlBuilder.PSC, InvoiceItemTypeAttrDao> {
}
