package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceTermEntity;

public interface InvoiceTermDao extends CrudDao<InvoiceTermEntity, String, SqlBuilder.PSC, InvoiceTermDao> {
}
