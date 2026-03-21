package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceRoleEntity;

public interface InvoiceRoleDao extends CrudDao<InvoiceRoleEntity, InvoiceRoleEntity, SqlBuilder.PSC, InvoiceRoleDao> {
}
