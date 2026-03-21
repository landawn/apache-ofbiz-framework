package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductMeterTypeEntity;

public interface ProductMeterTypeDao extends CrudDao<ProductMeterTypeEntity, String, SqlBuilder.PSC, ProductMeterTypeDao> {
}
