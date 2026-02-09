package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductMeterEntity;

public interface ProductMeterDao extends CrudDao<ProductMeterEntity, ProductMeterEntity, SQLBuilder.PSC, ProductMeterDao> {
}
