package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShippingDocumentEntity;

public interface ShippingDocumentDao extends CrudDao<ShippingDocumentEntity, String, SQLBuilder.PSC, ShippingDocumentDao> {
}
