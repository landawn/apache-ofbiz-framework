package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductManufacturingRuleEntity;

public interface ProductManufacturingRuleDao extends CrudDao<ProductManufacturingRuleEntity, String, SQLBuilder.PSC, ProductManufacturingRuleDao> {
}
