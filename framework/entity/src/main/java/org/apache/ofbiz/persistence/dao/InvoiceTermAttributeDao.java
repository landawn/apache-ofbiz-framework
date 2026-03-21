package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceTermAttributeEntity;

public interface InvoiceTermAttributeDao extends CrudDao<InvoiceTermAttributeEntity, InvoiceTermAttributeEntity, SqlBuilder.PSC, InvoiceTermAttributeDao> {
}
