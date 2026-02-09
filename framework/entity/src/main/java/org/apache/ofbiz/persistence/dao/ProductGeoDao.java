package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductGeoEntity;

public interface ProductGeoDao extends CrudDao<ProductGeoEntity, ProductGeoEntity, SQLBuilder.PSC, ProductGeoDao> {
}
