package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoRuleEntity;

public interface ProductPromoRuleDao extends CrudDao<ProductPromoRuleEntity, ProductPromoRuleEntity, SQLBuilder.PSC, ProductPromoRuleDao> {
}
