package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductTypeAttrEntity;

public interface ProductTypeAttrDao extends CrudDao<ProductTypeAttrEntity, ProductTypeAttrEntity, SQLBuilder.PSC, ProductTypeAttrDao> {
}
