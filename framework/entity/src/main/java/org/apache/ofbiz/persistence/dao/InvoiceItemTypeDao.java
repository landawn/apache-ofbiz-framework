package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeEntity;

public interface InvoiceItemTypeDao extends CrudDao<InvoiceItemTypeEntity, String, SQLBuilder.PSC, InvoiceItemTypeDao> {
}
