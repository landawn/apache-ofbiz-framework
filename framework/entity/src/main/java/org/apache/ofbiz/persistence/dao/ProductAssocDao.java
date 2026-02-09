package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductAssocEntity;

public interface ProductAssocDao extends CrudDao<ProductAssocEntity, ProductAssocEntity, SQLBuilder.PSC, ProductAssocDao> {
}
