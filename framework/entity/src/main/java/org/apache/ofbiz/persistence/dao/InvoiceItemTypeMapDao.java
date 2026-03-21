package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeMapEntity;

public interface InvoiceItemTypeMapDao extends CrudDao<InvoiceItemTypeMapEntity, InvoiceItemTypeMapEntity, SqlBuilder.PSC, InvoiceItemTypeMapDao> {
}
