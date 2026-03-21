package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeGlAccountEntity;

public interface InvoiceItemTypeGlAccountDao extends CrudDao<InvoiceItemTypeGlAccountEntity, InvoiceItemTypeGlAccountEntity, SqlBuilder.PSC, InvoiceItemTypeGlAccountDao> {
}
