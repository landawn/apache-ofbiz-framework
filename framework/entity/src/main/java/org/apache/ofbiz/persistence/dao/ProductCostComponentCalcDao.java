package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCostComponentCalcEntity;

public interface ProductCostComponentCalcDao extends CrudDao<ProductCostComponentCalcEntity, ProductCostComponentCalcEntity, SqlBuilder.PSC, ProductCostComponentCalcDao> , DelegatorQueryDao{
}
