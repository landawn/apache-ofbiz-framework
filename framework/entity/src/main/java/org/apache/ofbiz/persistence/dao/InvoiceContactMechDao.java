package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceContactMechEntity;

public interface InvoiceContactMechDao extends CrudDao<InvoiceContactMechEntity, InvoiceContactMechEntity, SQLBuilder.PSC, InvoiceContactMechDao> {
}
