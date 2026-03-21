package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceContentTypeEntity;

public interface InvoiceContentTypeDao extends CrudDao<InvoiceContentTypeEntity, String, SqlBuilder.PSC, InvoiceContentTypeDao> {
}
