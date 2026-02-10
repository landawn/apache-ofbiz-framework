package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductCalculatedInfoEntity;

public interface ProductCalculatedInfoDao extends CrudDao<ProductCalculatedInfoEntity, String, SQLBuilder.PSC, ProductCalculatedInfoDao>, DelegatorQueryDao {
}
