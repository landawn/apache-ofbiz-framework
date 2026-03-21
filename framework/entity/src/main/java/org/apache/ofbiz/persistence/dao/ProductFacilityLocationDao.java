package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFacilityLocationEntity;

public interface ProductFacilityLocationDao extends CrudDao<ProductFacilityLocationEntity, ProductFacilityLocationEntity, SqlBuilder.PSC, ProductFacilityLocationDao> {
}
