package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceTypeEntity;

public interface InvoiceTypeDao extends CrudDao<InvoiceTypeEntity, String, SqlBuilder.PSC, InvoiceTypeDao> {
}
