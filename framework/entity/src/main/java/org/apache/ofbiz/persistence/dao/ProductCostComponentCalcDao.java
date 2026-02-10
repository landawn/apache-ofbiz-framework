package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductCostComponentCalcEntity;

public interface ProductCostComponentCalcDao extends CrudDao<ProductCostComponentCalcEntity, ProductCostComponentCalcEntity, SQLBuilder.PSC, ProductCostComponentCalcDao> , DelegatorQueryDao{
}
