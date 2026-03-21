package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoRuleEntity;

public interface ProductPromoRuleDao extends CrudDao<ProductPromoRuleEntity, ProductPromoRuleEntity, SqlBuilder.PSC, ProductPromoRuleDao> {
}
