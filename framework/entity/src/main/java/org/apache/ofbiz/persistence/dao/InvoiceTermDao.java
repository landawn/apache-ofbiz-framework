package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceTermEntity;

public interface InvoiceTermDao extends CrudDao<InvoiceTermEntity, String, SQLBuilder.PSC, InvoiceTermDao> {
}
