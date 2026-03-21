package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreFacilityEntity;

public interface ProductStoreFacilityDao extends CrudDao<ProductStoreFacilityEntity, ProductStoreFacilityEntity, SqlBuilder.PSC, ProductStoreFacilityDao> {
}
