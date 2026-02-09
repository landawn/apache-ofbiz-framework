package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.InvoiceItemTypeMapEntity;

public interface InvoiceItemTypeMapDao extends CrudDao<InvoiceItemTypeMapEntity, InvoiceItemTypeMapEntity, SQLBuilder.PSC, InvoiceItemTypeMapDao> {
}
